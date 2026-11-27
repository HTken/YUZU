import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';
import { API_URL } from './config';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  const esApi = req.url.startsWith(API_URL);
  const esLogin = req.url.endsWith('/auth/login');
  const esLogout = req.url.endsWith('/auth/logout');

  // El token viaja en una cookie httpOnly: el navegador la manda solo si la petición pide "credentials".
  const peticion = esApi ? req.clone({ withCredentials: true }) : req;

  return next(peticion).pipe(
    catchError((err: HttpErrorResponse) => {
      // Token vencido o inválido: se cierra la sesión y vuelve al login.
      if (err.status === 401 && esApi && !esLogin && !esLogout) {
        auth.logout();
      }
      return throwError(() => err);
    }),
  );
};
