import {
  Component, inject, signal, effect, OnDestroy, DestroyRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { interval } from 'rxjs';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import {
  Chart,
  LineElement, PointElement, LineController,
  CategoryScale, LinearScale,
  Filler, Tooltip
} from 'chart.js';
import { AuthService } from '../../services/auth.service';
import { DashboardService } from '../../services/dashboard.service';
import { PresenceWebsocketService } from '../../services/presence-websocket.service';

Chart.register(
  LineElement, PointElement, LineController,
  CategoryScale, LinearScale,
  Filler, Tooltip
);

const MAX_HISTORY_MS = 60 * 60 * 1000; // 60 minutos
const SLOT_MS = 60_000; // 1 slot por minuto

interface HistoryPoint {
  timestamp: number;
  count: number;
}

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, BaseChartDirective],
  templateUrl: './dashboard.html',
})
export class DashboardComponent implements OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly dashboardService = inject(DashboardService);
  private readonly ws = inject(PresenceWebsocketService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isAdmin = this.auth.getRole() === 'ADMIN';

  // WebSocket
  readonly presenceCount = this.ws.presenceCount;
  readonly connectionStatus = this.ws.connectionStatus;

  // KPIs locais
  readonly peakCount = signal(0);
  readonly peakHour = signal<string | null>(null);
  readonly hasHistory = signal(false);

  // KPIs REST
  readonly totalAlunos = signal<number | null>(null);
  readonly totalFuncionarios = signal<number | null>(null);
  readonly funcionariosPresentes = signal<number | null>(null);

  // Histórico do gráfico (pontos com timestamp real)
  private historyPoints: HistoryPoint[] = [];

  // Chart.js data
  readonly chartData: ChartData<'line'> = {
    labels: [],
    datasets: [{
      data: [],
      label: 'Presentes',
      borderColor: '#0ea5e9',
      backgroundColor: 'rgba(14,165,233,0.15)',
      fill: true,
      tension: 0.3,
      pointRadius: 0,
      borderWidth: 2,
      spanGaps: true,
    }],
  };

  chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          title: (items) => items[0]?.label ?? '',
          label: (ctx) => ctx.parsed.y !== null ? ` ${ctx.parsed.y} alunos presentes` : ' sem dados',
        },
      },
    },
    scales: {
      x: {
        ticks: { color: '#64748b', maxTicksLimit: 8, maxRotation: 0 },
        grid: { color: '#334155' },
      },
      y: {
        min: 0,
        ticks: { color: '#64748b', stepSize: 10 },
        grid: { color: '#334155' },
      },
    },
  };

  constructor() {
    // Carrega histórico do backend ao inicializar
    const now = Date.now();
    this.dashboardService.getPresenceHistory(now - MAX_HISTORY_MS, now).subscribe({
      next: r => {
        this.historyPoints = r.points.map(p => ({ timestamp: p.timestamp, count: p.count }));
        // Recalcula pico com base no histórico carregado
        for (const p of this.historyPoints) {
          if (p.count > this.peakCount()) {
            this.peakCount.set(p.count);
            this.peakHour.set(new Date(p.timestamp).toLocaleTimeString('pt-BR', {
              hour: '2-digit', minute: '2-digit',
            }));
          }
        }
        this.buildChartWindow();
        this.hasHistory.set(this.historyPoints.length > 0);
      },
      error: () => {},
    });

    // Reage a cada update do WebSocket
    effect(() => {
      const count = this.presenceCount();
      if (count === null) return;

      const now = Date.now();
      const label = new Date(now).toLocaleTimeString('pt-BR', {
        hour: '2-digit', minute: '2-digit',
      });

      // Adiciona ponto e remove os mais velhos que 60min
      this.historyPoints.push({ timestamp: now, count });
      const cutoff = now - MAX_HISTORY_MS;
      this.historyPoints = this.historyPoints.filter(p => p.timestamp >= cutoff);

      // Atualiza pico
      if (count > this.peakCount()) {
        this.peakCount.set(count);
        this.peakHour.set(label);
      }

      // Atualiza chart com janela fixa
      this.buildChartWindow();
      this.hasHistory.set(true);

      // Eixo Y: pico visível + 20% de margem
      const visibleMax = Math.max(...this.historyPoints.map(p => p.count), 10);
      const yMax = Math.ceil(visibleMax * 1.2 / 10) * 10;
      this.chartOptions = {
        ...this.chartOptions,
        scales: {
          ...this.chartOptions!['scales'],
          y: { ...this.chartOptions!['scales']!['y'], min: 0, max: yMax },
        },
      };
    });

    // Carga inicial REST + polling 60s
    this.loadRestData();
    interval(60_000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadRestData());
  }

  private buildChartWindow(): void {
    const now = Date.now();
    const windowStart = now - MAX_HISTORY_MS;
    const labels: string[] = [];
    const slots: (number | null)[] = [];

    for (let t = windowStart; t <= now; t += SLOT_MS) {
      labels.push(new Date(t).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' }));
      // Ponto mais próximo dentro de ±30s do slot
      const nearest = this.historyPoints.find(p => Math.abs(p.timestamp - t) <= SLOT_MS / 2);
      slots.push(nearest ? nearest.count : null);
    }

    this.chartData.labels = labels;
    (this.chartData.datasets[0].data as (number | null)[]) = slots;
  }

  private loadRestData(): void {
    this.dashboardService.getAlunosCount().subscribe({
      next: r => this.totalAlunos.set(r.count),
      error: () => {},
    });

    if (this.isAdmin) {
      this.dashboardService.getFuncionariosCount().subscribe({
        next: r => this.totalFuncionarios.set(r.count),
        error: () => {},
      });

      this.dashboardService.getPresenceUsers().subscribe({
        next: r => {
          const funcCount = r.users.filter(u => u.userType === 'FUNCIONARIO').length;
          this.funcionariosPresentes.set(funcCount);
        },
        error: () => {},
      });
    }
  }

  ngOnDestroy(): void {}
}
