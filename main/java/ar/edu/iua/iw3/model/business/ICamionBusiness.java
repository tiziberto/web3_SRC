package ar.edu.iua.iw3.model.business;

import ar.edu.iua.iw3.model.Camion;

public interface ICamionBusiness {
    Camion load(long id) throws NotFoundException, BusinessException;
    Camion load(String codExterno) throws NotFoundException, BusinessException;
    Camion add(Camion camion) throws FoundException, BusinessException;
    Camion update(Camion camion) throws NotFoundException, BusinessException;
    void delete(long id) throws NotFoundException, BusinessException;
}