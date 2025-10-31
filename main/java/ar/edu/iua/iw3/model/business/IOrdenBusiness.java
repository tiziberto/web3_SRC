package ar.edu.iua.iw3.model.business;

import ar.edu.iua.iw3.model.Orden;

public interface IOrdenBusiness {
    
    // Métodos Punto 1
    public Orden load(long id) throws NotFoundException, BusinessException;
    public Orden load(int numeroOrden) throws NotFoundException, BusinessException;
    public Orden add(Orden orden) throws FoundException, BusinessException;

    // Método Punto 2 (CORREGIDO: DEBE DECLARAR LAS EXCEPCIONES)
    public Orden registerInitialWeighing(int numeroOrden, double pesajeInicial) throws NotFoundException, BusinessException;
}