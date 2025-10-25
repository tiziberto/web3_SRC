package ar.edu.iua.iw3.model.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.iua.iw3.model.Chofer;

@Repository
public interface ChoferRepository extends JpaRepository<Chofer, Long> {
    
    // Búsqueda por el código de integración externo (SAP/TMS)
    Optional<Chofer> findOneByCodExterno(String codExterno);

    // Búsqueda por documento (opcional según el PDF, pero útil como identificador)
    Optional<Chofer> findOneByDocumento(String documento);
}