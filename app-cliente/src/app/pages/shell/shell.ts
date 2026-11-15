import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/auth.service';
import { MiPlan, PlanService, textoVencimiento } from '../../core/plan.service';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class Shell implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly planes = inject(PlanService);
  protected readonly textoVencimiento = textoVencimiento;

  /** Solo se usa en pantallas angostas, donde el menú se abre y se cierra. */
  protected readonly menuAbierto = signal(false);

  ngOnInit(): void {
    this.planes.cargar();
  }

  protected cerrarMenu(): void {
    this.menuAbierto.set(false);
  }

  protected nivel(p: MiPlan): 'ok' | 'aviso' | 'critico' {
    if (p.estado === 'SUSPENDIDO' || p.diasRestantes < 0) {
      return 'critico';
    }
    if (p.estado === 'EN_GRACIA' || p.diasRestantes <= 7 || p.usuariosActivos >= p.maxUsuarios) {
      return 'aviso';
    }
    return 'ok';
  }
}
