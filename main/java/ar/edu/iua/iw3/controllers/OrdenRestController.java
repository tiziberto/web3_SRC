package ar.edu.iua.iw3.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping; // <-- NUEVO IMPORT
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
// Mapea a: /api/v1/ordenes
@RequestMapping("/api/v1/ordenes") 
@Tag(description = "API para la administración de Órdenes de Carga", name = "Orden")
public class OrdenRestController extends BaseRestController {

    // Método de prueba GET
    // Mapea a: GET /api/v1/ordenes
    @GetMapping(value = "") // <-- MÉTODO AÑADIDO
    public ResponseEntity<String> testGetMapping() { 
        return new ResponseEntity<>("GET Endpoint de Ordenes funcionando!", HttpStatus.OK); 
    }

    // Método de prueba POST
    // Mapea a: POST /api/v1/ordenes
    @PostMapping(value = "")
    public ResponseEntity<?> add() {
        return new ResponseEntity<>("POST Endpoint de Ordenes funcionando!", HttpStatus.OK); 
    }
}