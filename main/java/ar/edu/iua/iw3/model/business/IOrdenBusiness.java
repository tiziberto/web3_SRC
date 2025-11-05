package ar.edu.iua.iw3.model.business;

import ar.edu.iua.iw3.model.ConciliacionDTO;
import ar.edu.iua.iw3.model.DetalleCargaDTO;
import ar.edu.iua.iw3.model.FlowStartDTO;
import ar.edu.iua.iw3.model.Orden;

public interface IOrdenBusiness {
    
    // Métodos Punto 1
    public Orden load(long id) throws NotFoundException, BusinessException;
    public Orden load(int numeroOrden) throws NotFoundException, BusinessException;
    public Orden add(Orden orden) throws FoundException, BusinessException;

    // Método Punto 2 (CORREGIDO: DEBE DECLARAR LAS EXCEPCIONES)
    public Orden registerInitialWeighing(int numeroOrden, double pesajeInicial) throws NotFoundException, BusinessException;

    // --- PUNTO 3: RECEPCIÓN DE DATOS CONTINUOS ---
    public Orden receiveRealTimeData(DetalleCargaDTO data) throws NotFoundException, BusinessException;

    // Nuevo método para el Punto 4: Cierre de la Orden
    public Orden closeOrder(int numeroOrden) throws NotFoundException, BusinessException;

    // --- PUNTO 5: CONCILIACIÓN (ESTA LÍNEA RESUELVE EL ERROR) ---
    public ConciliacionDTO getConciliacion(int numeroOrden) 
        throws NotFoundException, BusinessException;
    
    // --- PUNTO 5: REGISTRO DE PESAJE FINAL ---
    public Orden registerFinalWeighing(int numeroOrden, double pesajeFinal) 
        throws NotFoundException, BusinessException;
}