import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/**
 * Solo deja entrar a una pantalla a quien tenga alguno de esos roles; a los demás los lleva al inicio.
 * Es solo comodidad: quien de verdad protege los datos es el servidor, que también valida el rol.
 */
export function soloRoles(...roles: string[]): CanActivateFn {
  return () => {
    const rol = inject(AuthService).sesion()?.rol;
    return rol !== undefined && roles.includes(rol) ? true : inject(Router).createUrlTree(['/']);
  };
}
