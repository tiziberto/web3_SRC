package ar.edu.iua.iw3.model.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.iua.iw3.model.DetalleCarga;

@Repository
public interface DetalleCargaRepository extends JpaRepository<DetalleCarga, Long> {
    // Métodos estándar CRUD. Las consultas más complejas se harían en el Business (ej: buscar por id_orden)
}