package ar.edu.iua.iw3.model.business;

import java.util.Date;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ar.edu.iua.iw3.model.Orden;
import ar.edu.iua.iw3.model.EstadoOrden;
import ar.edu.iua.iw3.model.persistence.OrdenRepository;
// CORREGIDO: la importación debe ser slf4j, no slf44j
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
}