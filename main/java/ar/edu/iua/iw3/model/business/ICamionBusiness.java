package ar.edu.iua.iw3.model.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import ar.edu.iua.iw3.model.Camion;
import ar.edu.iua.iw3.model.persistence.ICamionRepository;

public interface ICamionBusiness {
    Camion load(long id) throws NotFoundException, BusinessException;
    Camion load(String codExterno) throws NotFoundException, BusinessException;
    Camion add(Camion camion) throws FoundException, BusinessException;
    Camion update(Camion camion) throws NotFoundException, BusinessException;
    Camion save(Camion camion) throws BusinessException;
    void delete(long id) throws NotFoundException, BusinessException;
    Camion loadByPatente(String patente) throws NotFoundException, BusinessException;
}