package ar.edu.iua.iw3.model;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Importante para la deserialización de fechas del JSON de Postman
import com.fasterxml.jackson.annotation.JsonFormat; 

@Entity
@Table(name = "ordenes")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Orden implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Atributos principales
	@Column(unique = true, nullable = false)
	private Integer numeroOrden;

	@Enumerated(EnumType.ORDINAL) // Mapeo del Enum al índice (0, 1, 2, 3...)
	@Column(nullable = false)
	private EstadoOrden estado = EstadoOrden.ESTADO_1_PENDIENTE_PESAJE_INICIAL;
	
	// Relaciones ManyToOne: Un error aquí puede causar el fallo de inicio
	@ManyToOne
	@JoinColumn(name = "id_camion", nullable = false)
	private Camion camion; 

	@ManyToOne
	@JoinColumn(name = "id_chofer", nullable = false)
	private Chofer chofer;

	@ManyToOne
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@ManyToOne
	@JoinColumn(name = "id_producto", nullable = false)
	private Producto producto;

	// Valores Base y Fechas de Proceso
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ") // <- IMPORTANTE
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaCargaPrevista; 
	
	private Double preset; 
	
	// Fechas de Proceso (se llenan en el backend)
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaRecepcionInicial;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaPesajeInicial;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaInicioCarga;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaFinCarga;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaPesajeFinal;

	// Datos del proceso (Estado 2)
	private Double pesajeInicial; 
	private Double pesajeFinal;
	private String passwordActivacion; 
	
	// Últimos valores de cabecera
	private Double ultimaMasaAcumulada;
	private Double ultimaDensidad;
	private Double ultimaTemperatura;
	private Double ultimoCaudal;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date estampaTiempoUltimoDato;
}