package ar.edu.iua.iw3.model.business;

import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ar.edu.iua.iw3.model.Orden;
import ar.edu.iua.iw3.model.EstadoOrden;
import ar.edu.iua.iw3.model.FlowStartDTO;
import ar.edu.iua.iw3.model.persistence.OrdenRepository;
import ar.edu.iua.iw3.model.DetalleCargaDTO;
import ar.edu.iua.iw3.model.DetalleCarga;
import ar.edu.iua.iw3.model.persistence.DetalleCargaRepository;
import ar.edu.iua.iw3.model.ConciliacionDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j; 

@Service
@Slf4j
public class OrdenBusiness implements IOrdenBusiness {

    @Autowired
    private OrdenRepository ordenDAO;
    
    // Dependencias inyectadas: SI ALGUNA DE ESTAS FALLA, EL CONTEXTO NO CARGA
    @Autowired 
    private ICamionBusiness camionBusiness;
    @Autowired 
    private IChoferBusiness choferBusiness;
    @Autowired 
    private IClienteBusiness clienteBusiness;
    @Autowired 
    private IProductoBusiness productoBusiness;
    
    // Métodos load

    @Override
    public Orden load(long id) throws NotFoundException, BusinessException {
        Optional<Orden> r;
        try {
            r = ordenDAO.findById(id);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw BusinessException.builder().ex(e).build();
        }
        if (r.isEmpty()) {
            throw NotFoundException.builder().message("No se encuentra la Orden id=" + id).build();
        }
        return r.get();
    }

    @Override
    public Orden load(int numeroOrden) throws NotFoundException, BusinessException {
        Optional<Orden> r;
        try {
            r = ordenDAO.findOneByNumeroOrden(numeroOrden);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw BusinessException.builder().ex(e).build();
        }
        if (r.isEmpty()) {
            throw NotFoundException.builder().message("No se encuentra la Orden Nro=" + numeroOrden).build();
        }
        return r.get();
    }
    
    // Método Punto 1: Recepción de datos base
    @Override
    public Orden add(Orden orden) throws FoundException, BusinessException {
        if (ordenDAO.findOneByNumeroOrden(orden.getNumeroOrden()).isPresent()) {
            throw FoundException.builder().message("La Orden Nro=" + orden.getNumeroOrden() + " ya existe.").build();
        }

        // Validación de existencia de las entidades base
        try {
            camionBusiness.load(orden.getCamion().getId());
            choferBusiness.load(orden.getChofer().getId());
            clienteBusiness.load(orden.getCliente().getId());
            productoBusiness.load(orden.getProducto().getId());
        } catch (NotFoundException e) {
            throw BusinessException.builder().message("Error en entidad asociada: " + e.getMessage()).build();
        }

        // Setear estado inicial y fecha de recepción
        orden.setEstado(EstadoOrden.ESTADO_1_PENDIENTE_PESAJE_INICIAL);
        orden.setFechaRecepcionInicial(new Date());

        try {
            return ordenDAO.save(orden);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw BusinessException.builder().ex(e).build();
        }
    }
    
    // Método Punto 2: Registro de tara
    @Override
    public Orden registerInitialWeighing(int numeroOrden, double pesajeInicial) 
            throws NotFoundException, BusinessException {
        
        Orden orden = load(numeroOrden);

        if (orden.getEstado() != EstadoOrden.ESTADO_1_PENDIENTE_PESAJE_INICIAL) {
            throw BusinessException.builder()
                .message("La Orden Nro=" + numeroOrden + " no está en estado 'Pendiente de pesaje inicial'. Estado actual: " + orden.getEstado().getDescripcion())
                .build();
        }

        orden.setPesajeInicial(pesajeInicial);
        orden.setFechaPesajeInicial(new Date());
        
        Random random = new Random();
        int password = 10000 + random.nextInt(90000);
        orden.setPasswordActivacion(String.valueOf(password));

        orden.setEstado(EstadoOrden.ESTADO_2_CON_PESAJE_INICIAL_REGISTRADO);

        try {
            log.info("Orden {} pasa a Estado 2. Password: {}", numeroOrden, password);
            return ordenDAO.save(orden);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw BusinessException.builder().ex(e).build();
        }
    }


    @Autowired
    private DetalleCargaRepository detalleCargaDAO; // <- Inyectar Repositorio de Detalle

    // Método de soporte para buscar la Orden por contraseña
    private Orden loadByPassword(String password) throws NotFoundException, BusinessException {
        Optional<Orden> ou;
        try {
            ou = ordenDAO.findOneByPasswordActivacion(password);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw BusinessException.builder().ex(e).build();
        }
        if (ou.isEmpty()) {
            throw NotFoundException.builder().message("No se encuentra Orden activa para la password.").build();
        }
        return ou.get();
    }

    // Implementación del Punto 3
    private Double handleFlowStart(Orden orden, String passwordActivacion) throws BusinessException {
        // 1. Validar estado: Solo si está en Estado 2
        if (orden.getEstado() != EstadoOrden.ESTADO_2_CON_PESAJE_INICIAL_REGISTRADO) {
            throw BusinessException.builder()
                .message("La Orden Nro=" + orden.getNumeroOrden() + " no está lista para iniciar la carga (Estado 2). Estado actual: " + orden.getEstado().getDescripcion())
                .build();
        }

        // 2. Verificar Contraseña
        if (!orden.getPasswordActivacion().equals(passwordActivacion)) {
            throw BusinessException.builder().message("Contraseña de activación incorrecta.").build();
        }

        // 3. Retornar Preset
        return orden.getPreset();
    }


    // --- IMPLEMENTACIÓN DEL PUNTO 3 UNIFICADO ---
    
    @Override
    public Orden receiveRealTimeData(DetalleCargaDTO data) throws NotFoundException, BusinessException {
        //Comprobar que la orden exista
        Orden orden = load(data.getNumeroOrden());
        
        // 1. Validar estado: Debe ser Estado 2
        if (orden.getEstado() != EstadoOrden.ESTADO_2_CON_PESAJE_INICIAL_REGISTRADO) {
            throw BusinessException.builder()
                .message("La Orden Nro=" + data.getNumeroOrden() + " no está lista para iniciar la carga (Estado 2). Estado actual: " + orden.getEstado().getDescripcion())
                .build();
        }

        // 2. Verificar Contraseña
        if (!orden.getPasswordActivacion().equals(data.getPasswordActivacion())) {
            throw BusinessException.builder().message("Contraseña de activación incorrecta.").build();
        }

        // El dispositivo debe enviar la contraseña en el DTO
        if (data.getPasswordActivacion() == null) {
            throw BusinessException.builder().message("La petición de flujo continuo requiere 'passwordActivacion'.").build();
        }
        
        boolean datosValidos = isRealTimeDataValid(orden, data);

        if (datosValidos) {
            // 2. Procesar datos: Actualizar cabecera, persistir detalle y chequear alarma
            updateOrderHeader(orden, data);
            persistDetail(orden, data);
            
            try {
                return ordenDAO.save(orden);
            } catch (Exception e) {
                log.error("Error al guardar la orden actualizada: {}", e.getMessage(), e);
                throw BusinessException.builder().ex(e).build();
            }

        } else {
            log.warn("Datos recibidos para Orden {} descartados por no ser válidos.", orden.getNumeroOrden());
            return orden; // Devuelve la orden sin modificar si los datos no son válidos
        }
    }

    
    // Auxiliar: Lógica de Validación de Datos de Flujo
    private boolean isRealTimeDataValid(Orden orden, DetalleCargaDTO data) {
        
        // Criterios de descarte: Caudal ≤ 0 o Masa acumulada ≤ 0
        if (data.getCaudal() == null || data.getCaudal() <= 0) {
            return false; 
        }
        
        if (data.getMasaAcumulada() == null || data.getMasaAcumulada() <= 0) {
            return false; 
        }
        
        // Masa acumulada debe ser mayor que el valor anterior
        if (orden.getUltimaMasaAcumulada() != null && data.getMasaAcumulada() < orden.getUltimaMasaAcumulada()) {
            return false; 
        }
        
        return true;
    }

    // Auxiliar: Lógica de Actualización de Cabecera
    private void updateOrderHeader(Orden orden, DetalleCargaDTO data) {
        Date now = new Date();
        
        if (orden.getFechaInicioCarga() == null) {
            orden.setFechaInicioCarga(now); 
        }
        
        // Actualizar los últimos valores en la cabecera
        orden.setUltimaMasaAcumulada(data.getMasaAcumulada());
        orden.setUltimaDensidad(data.getDensidad());
        orden.setUltimaTemperatura(data.getTemperatura());
        orden.setUltimoCaudal(data.getCaudal());
        orden.setEstampaTiempoUltimoDato(now);
    }
    
    // Auxiliar: Lógica de Persistencia de Detalle
    private void persistDetail(Orden orden, DetalleCargaDTO data) throws BusinessException {
        DetalleCarga detalle = new DetalleCarga();
        detalle.setOrden(orden);
        detalle.setMasaAcumulada(data.getMasaAcumulada());
        detalle.setDensidad(data.getDensidad());
        detalle.setTemperatura(data.getTemperatura());
        detalle.setCaudal(data.getCaudal());
        detalle.setEstampaTiempo(orden.getEstampaTiempoUltimoDato());

        try {
            detalleCargaDAO.save(detalle);
        } catch (Exception e) {
            log.error("Error al guardar detalle de carga: {}", e.getMessage(), e);
            throw BusinessException.builder().message("Fallo al persistir detalle de carga.").build();
        }
    }

    // Implementación del Punto 4: Cierre de la Orden
    @Override
    public Orden closeOrder(int numeroOrden) throws NotFoundException, BusinessException {
        
        Orden orden = load(numeroOrden); // Verifica si la orden existe

        // 1. Validar estado: Solo se puede cerrar si está en Estado 2 (en proceso de carga)
        if (orden.getEstado() != EstadoOrden.ESTADO_2_CON_PESAJE_INICIAL_REGISTRADO) {
            throw BusinessException.builder()
                .message("La Orden Nro=" + numeroOrden + " no está activa para ser cerrada. Estado actual: " + orden.getEstado().getDescripcion())
                .build();
        }

        // 2. Registrar Fecha de Fin de Carga (momento del cierre)
        // La fecha de fin de carga es el momento del último dato válido (Punto 3) o el momento del cierre.
        // Aquí usamos la fecha del sistema, ya que esta es la acción de "cierre".
        orden.setFechaFinCarga(new Date()); 
        
        // 3. Cambiar estado a Estado 3
        orden.setEstado(EstadoOrden.ESTADO_3_CERRADA_PARA_CARGA);

        // 4. Guardar
        try {
            log.info("Orden {} pasa a Estado 3: Cerrada para carga.", numeroOrden);
            return ordenDAO.save(orden);
        } catch (Exception e) {
            log.error("Error al cerrar la orden: {}", e.getMessage(), e);
            throw BusinessException.builder().ex(e).build();
        }
    }

    @PersistenceContext // Inyección para consultas de BD avanzadas
    private EntityManager entityManager;

    // --- PUNTO 5 - PARTE 1: Registrar Pesaje Final (PUT) ---
    @Override
    public Orden registerFinalWeighing(int numeroOrden, double pesajeFinal) throws NotFoundException, BusinessException {
        Orden orden = load(numeroOrden);

        // 1. Validar estado: Solo si está en Estado 3
        if (orden.getEstado() != EstadoOrden.ESTADO_3_CERRADA_PARA_CARGA) {
            throw BusinessException.builder()
                .message("La Orden Nro=" + numeroOrden + " no está cerrada. Estado actual: " + orden.getEstado().getDescripcion())
                .build();
        }
        
        // 2. Almacenar Pesaje Final y Fecha
        orden.setPesajeFinal(pesajeFinal);
        orden.setFechaPesajeFinal(new Date());

        // 3. Cambiar estado a Estado 4
        orden.setEstado(EstadoOrden.ESTADO_4_FINALIZADA);

        // 4. Guardar
        try {
            log.info("Orden {} pasa a Estado 4: Finalizada. Pesaje final: {}", numeroOrden, pesajeFinal);
            return ordenDAO.save(orden);
        } catch (Exception e) {
            log.error("Error al registrar pesaje final: {}", e.getMessage(), e);
            throw BusinessException.builder().ex(e).build();
        }
    }


    // --- PUNTO 5 - PARTE 2: Obtener Conciliación (GET) ---
    @Override
    public ConciliacionDTO getConciliacion(int numeroOrden) throws NotFoundException, BusinessException {
        Orden orden = load(numeroOrden);
        
        // 1. Validar estado: Solo se puede solicitar si está en Estado 4
        if (orden.getEstado() != EstadoOrden.ESTADO_4_FINALIZADA) {
            throw BusinessException.builder()
                .message("La Orden Nro=" + numeroOrden + " no está finalizada. Estado actual: " + orden.getEstado().getDescripcion())
                .build();
        }
        
        if (orden.getPesajeInicial() == null || orden.getPesajeFinal() == null || orden.getUltimaMasaAcumulada() == null) {
             throw BusinessException.builder().message("Faltan datos de pesaje o carga para la conciliación.").build();
        }
        
        // 2. Cálculo de Neto y Diferencia
        double netoBalanza = orden.getPesajeFinal() - orden.getPesajeInicial();
        double diferenciaBalanzaCaudalimetro = netoBalanza - orden.getUltimaMasaAcumulada();

        // 3. Cálculo de Promedios (Usando el EntityManager)
        Object[] promedios = calculateAverages(orden.getId());
        
        // 4. Construir DTO
        return ConciliacionDTO.builder()
                .numeroOrden(numeroOrden)
                .pesajeInicial(orden.getPesajeInicial())
                .pesajeFinal(orden.getPesajeFinal())
                .productoCargado(orden.getUltimaMasaAcumulada())
                .netoBalanza(netoBalanza)
                .diferenciaBalanzaCaudalimetro(diferenciaBalanzaCaudalimetro)
                .promedioTemperatura((Double) promedios[0])
                .promedioDensidad((Double) promedios[1])
                .promedioCaudal((Double) promedios[2])
                .build();
    }
    
    // Método para calcular promedios de detalle
    private Object[] calculateAverages(Long ordenId) throws BusinessException {
        try {
            // Consulta nativa para obtener los promedios (AVG)
            String sql = "SELECT AVG(temperatura), AVG(densidad), AVG(caudal) FROM detalle_cargas WHERE id_orden = :ordenId";
            
            Object[] result = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("ordenId", ordenId)
                .getSingleResult();

            // Casteamos los resultados (result[0]=Temp, result[1]=Dens, result[2]=Caudal)
            Double avgTemp = result[0] != null ? ((Number) result[0]).doubleValue() : null;
            Double avgDens = result[1] != null ? ((Number) result[1]).doubleValue() : null;
            Double avgCaudal = result[2] != null ? ((Number) result[2]).doubleValue() : null;

            return new Object[]{avgTemp, avgDens, avgCaudal};
            
        } catch (Exception e) {
            log.error("Error al calcular promedios para la orden {}: {}", ordenId, e.getMessage());
            throw BusinessException.builder().message("Error calculando promedios de detalle.").build();
        }
    }
}