import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('renderiza o shell com os 4 itens de navegação', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const links: NodeListOf<HTMLAnchorElement> =
      fixture.nativeElement.querySelectorAll('a.nav-link');
    const labels = Array.from(links).map((a) => a.textContent?.trim());

    expect(labels).toEqual(['Dashboard', 'Sessões', 'Planejamento', 'Catálogo']);
  });

  it('tem um botão para alternar o tema', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const button: HTMLButtonElement | null =
      fixture.nativeElement.querySelector('button.theme-toggle');

    expect(button).not.toBeNull();
  });
});
