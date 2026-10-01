import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ThemeService } from '../theme-service';

interface NavItem {
  path: string;
  label: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly themeService = inject(ThemeService);

  protected readonly navItems: NavItem[] = [
    { path: '/dashboard', label: 'Dashboard' },
    { path: '/sessions', label: 'Sessões' },
    { path: '/planning', label: 'Planejamento' },
    { path: '/catalog', label: 'Catálogo' },
  ];
}
