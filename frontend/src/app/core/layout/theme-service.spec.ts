import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme-service';

describe('ThemeService',() => {
  beforeEach(() => {
    localStorage.clear();
    delete document.documentElement.dataset['theme'];
    TestBed.configureTestingModule({});
    });

  it('usa o tema salvo no localStorage', () => {
    localStorage.setItem('studyos.theme', 'dark');
    const service = TestBed.inject(ThemeService);
    expect(service.theme()).toBe('dark');
    });

  it('alterna entre claro e escuro', () => {
    localStorage.setItem('studyos.theme', 'light');
    const service = TestBed.inject(ThemeService);

    service.toggle();
    expect(service.theme()).toBe('dark');

    service.toggle();
    expect(service.theme()).toBe('light');
    });

  it('aplica o tema no <html> e salva a escolha', () => {
    localStorage.setItem('studyos.theme', 'light');
    const service = TestBed.inject(ThemeService);

    service.toggle();
    TestBed.tick();

    expect(document.documentElement.dataset['theme']).toBe('dark');
    expect(localStorage.getItem('studyos.theme')).toBe('dark');
    });

  it('ignora valor inválido salvo', () => {
    localStorage.setItem('studyos.theme', 'roxo');
    const service = TestBed.inject(ThemeService);
    expect(['light', 'dark']).toContain(service.theme());
    });

  it('sem valor salvo e sem matchMedia, usa light', () => {
    const original = window.matchMedia;
    Object.defineProperty(window, 'matchMedia', { value:
      undefined, configurable: true });

      const service = TestBed.inject(ThemeService);
      expect(service.theme()).toBe('light');

      Object.defineProperty(window, 'matchMedia', { value:
        original, configurable: true });
        });
      });
