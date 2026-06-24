package com.tav.referance_manager.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BusinessException → 409 CONFLICT + mesaj")
    void handleBusiness_returns409() {
        ResponseEntity<Map<String, Object>> r =
                handler.handleBusiness(new BusinessException("Duplicate code"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r.getBody()).containsEntry("error", "Duplicate code");
        assertThat(r.getBody()).containsEntry("status", 409);
    }

    @Test
    @DisplayName("NotFoundException → 404 NOT_FOUND + mesaj")
    void handleNotFound_returns404() {
        ResponseEntity<Map<String, Object>> r =
                handler.handleNotFound(new NotFoundException("Havayolu bulunamadı"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody()).containsEntry("error", "Havayolu bulunamadı");
    }

    @Test
    @DisplayName("BadCredentialsException → 401 + sabit Türkçe mesaj")
    void handleBadCredentials_returns401() {
        ResponseEntity<Map<String, Object>> r =
                handler.handleBadCredentials(new BadCredentialsException("ignored"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(r.getBody()).containsEntry("error", "Kullanıcı adı veya parola hatalı");
    }

    @Test
    @DisplayName("AccessDeniedException → 403 + 'Yetkisiz işlem'")
    void handleAccessDenied_returns403() {
        ResponseEntity<Map<String, Object>> r =
                handler.handleAccessDenied(new AccessDeniedException("denied"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r.getBody()).containsEntry("error", "Yetkisiz işlem");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 + fields map")
    void handleValidation_returns400WithFieldErrors() throws Exception {
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "airlineRequest");
        br.addError(new FieldError("airlineRequest", "code", "IATA kodu 2 büyük harf olmalı"));
        br.addError(new FieldError("airlineRequest", "name", "olmamalı boş"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new MethodParameter(
                        GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy"), -1),
                br);

        ResponseEntity<Map<String, Object>> r = handler.handleValidation(ex);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r.getBody()).containsEntry("status", 400);
        assertThat(r.getBody()).containsEntry("error", "Doğrulama hatası");

        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) r.getBody().get("fields");
        assertThat(fields)
                .containsEntry("code", "IATA kodu 2 büyük harf olmalı")
                .containsEntry("name", "olmamalı boş");
    }

    @Test
    @DisplayName("Validation → defaultMessage null ise fallback 'Geçersiz değer'")
    void handleValidation_nullDefaultMessage_fallbacks() throws Exception {
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "airlineRequest");
        br.addError(new FieldError("airlineRequest", "code", null));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                new MethodParameter(
                        GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy"), -1),
                br);

        ResponseEntity<Map<String, Object>> r = handler.handleValidation(ex);

        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) r.getBody().get("fields");
        assertThat(fields).containsEntry("code", "Geçersiz değer");
    }

    @SuppressWarnings("unused")
    private void dummy() {}
}
