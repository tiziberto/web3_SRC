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

	@Enumerated(EnumType.ORDINAL)
	@Column(nullable = false)
	private EstadoOrden estado = EstadoOrden.ESTADO_1_PENDIENTE_PESAJE_INICIAL;
	
	// Relaciones
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

	// Valores Base
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaCargaPrevista; // Turno de carga
	
	private Double preset; // Cantidad de kilogramos a cargar
	
	// Fechas del proceso
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
	private Double pesajeInicial; // Tara
	private Double pesajeFinal;
	private String passwordActivacion; // Contraseña de 5 dígitos para habilitar el instrumento de carga
	
	// Últimos valores de cabecera (Página 5 del PDF)
	private Double ultimaMasaAcumulada;
	private Double ultimaDensidad;
	private Double ultimaTemperatura;
	private Double ultimoCaudal;
	
	@Temporal(TemporalType.TIMESTAMP)
	private Date estampaTiempoUltimoDato;
}