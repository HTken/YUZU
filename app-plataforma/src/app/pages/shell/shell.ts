import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './shell.html',
  styleUrls: ['./shell.css', './shell-cuenta.css'],
})
export class Shell {
  protected readonly auth = inject(AuthService);
}
