package pe.marcona.mtc.service.impl;

import com.google.common.base.Preconditions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import pe.marcona.mtc.dao.ILimitePeticionesDao;
import pe.marcona.mtc.exception.ApiException;
import pe.marcona.mtc.service.ILimitePeticionesService;

/**
 * Implementación de la lógica de rate limiting inyectando {@link ILimitePeticionesDao}.
 */
@Service
public class LimitePeticionesServiceImpl implements ILimitePeticionesService {

    private final ILimitePeticionesDao limitePeticionesDao;

    public LimitePeticionesServiceImpl(ILimitePeticionesDao limitePeticionesDao) {
        this.limitePeticionesDao = Preconditions.checkNotNull(limitePeticionesDao, "El DAO de límite de peticiones no puede ser nulo");
    }

    @Override
    public void verificar(String accion, String ip, int maximoPermitido, int segundosVentana) {
        int conteo = limitePeticionesDao.contarPeticionesEnVentana(ip, accion, segundosVentana);

        if (conteo >= maximoPermitido) {
            Long restantes = limitePeticionesDao.obtenerSegundosRestantes(ip, accion, segundosVentana);
            long reintentarEn = Math.max(1, restantes == null ? segundosVentana : restantes);
            long minutos = (reintentarEn + 59) / 60;

            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiadas solicitudes. Por favor espera " + minutos + " minuto(s) antes de intentar nuevamente.",
                    reintentarEn
            );
        }

        limitePeticionesDao.registrarPeticion(ip, accion);
    }
}

