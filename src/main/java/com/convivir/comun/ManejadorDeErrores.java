package com.convivir.comun;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte las excepciones del dominio en respuestas HTTP con un mensaje que
 * explica y dirige, como pide el sistema de diseño. Los códigos son los de los
 * diagramas de secuencia: 404, 409 y 422.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    @ExceptionHandler(NoEncontradoException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(NoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NoAutorizadoException.class)
    public ResponseEntity<Map<String, String>> noAutorizado(NoAutorizadoException e) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, String>> conflicto(ConflictoException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<Map<String, String>> regla(ReglaDeNegocioException e) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Revisa los datos enviados.");
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** Un dato único repetido que pasó la revisión del servicio (dos personas a la vez, o distinto en mayúsculas). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> datoRepetidoEnLaBase(DataIntegrityViolationException e) {
        return responder(HttpStatus.CONFLICT,
                "Ese dato ya está registrado o depende de otro registro. Revisa la lista y vuelve a intentarlo.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST,
                "No se pudieron leer los datos enviados. Revisa que sea JSON en UTF-8 y que cada valor tenga su tipo: "
                        + "números sin comillas, sí o no como true o false, y estados escritos como en la lista.");
    }

    private ResponseEntity<Map<String, String>> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", mensaje));
    }
}
