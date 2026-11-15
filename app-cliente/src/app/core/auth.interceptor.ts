import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';
import { API_URL } from './config';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.sesion()?.token;

  const esApi = req.url.startsWith(API_URL);
  const esLogin = req.url.endsWith('/api/auth/login');

  const peticion =
    token && esApi && !esLogin
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(peticion).pipe(
    catchError((err: HttpErrorResponse) => {
      // Token vencido o inválido: se cierra la sesión y vuelve al login.
      if (err.status === 401 && esApi && !esLogin) {
        auth.logout();
      }
      return throwError(() => err);
    }),
  );
};
