import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell {
  protected readonly auth = inject(AuthService);

  /** Solo se usa en pantallas angostas, donde el menú se abre y se cierra. */
  protected readonly menuAbierto = signal(false);

  protected cerrarMenu(): void {
    this.menuAbierto.set(false);
  }
}
