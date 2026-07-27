import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnalyticsService, AnalyticsOverview } from './services/analytics.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="admin-container">
      <!-- Top Navigation Header -->
      <header class="glass-panel header">
        <div class="logo-area">
          <div class="logo-icon">FH</div>
          <div>
            <h1 class="logo-title">FriendsHub Admin</h1>
            <p class="logo-sub">Polyglot Microservices & MongoDB Portal</p>
          </div>
        </div>
        <div class="header-status">
          <span class="badge badge-success">
            <span class="pulse-dot"></span> System Online
          </span>
          <span class="badge badge-purple">
            Angular 17 Dashboard
          </span>
        </div>
      </header>

      <!-- Main Dashboard Grid -->
      <main class="main-content">
        <!-- Stat Cards Row -->
        <div class="stats-grid">
          <div class="glass-panel stat-card">
            <div class="stat-header">
              <span class="stat-title">MongoDB Messages</span>
              <span class="badge badge-purple">Mongo Atlas</span>
            </div>
            <div class="stat-value">{{ data?.metrics?.totalMongoMessages || 0 }}</div>
            <div class="stat-desc">Low-latency MongoDB document writes</div>
          </div>

          <div class="glass-panel stat-card">
            <div class="stat-header">
              <span class="stat-title">Active Platform Users</span>
              <span class="badge badge-success">Live</span>
            </div>
            <div class="stat-value">{{ data?.metrics?.totalActiveUsers || 0 }}</div>
            <div class="stat-desc">Spring Boot & React concurrent sessions</div>
          </div>

          <div class="glass-panel stat-card">
            <div class="stat-header">
              <span class="stat-title">Async Event Latency</span>
              <span class="badge badge-success">Optimal</span>
            </div>
            <div class="stat-value">{{ data?.metrics?.systemLatencyMs || 0 }}ms</div>
            <div class="stat-desc">Node.js Express non-blocking I/O</div>
          </div>
        </div>

        <!-- Microservice Architecture Status -->
        <section class="glass-panel arch-section">
          <h2 class="section-title">Active Polyglot Stack</h2>
          <div class="services-list">
            <div class="service-chip" *ngFor="let service of data?.metrics?.activeServices">
              <span class="chip-dot"></span>
              <span>{{ service }}</span>
            </div>
          </div>
        </section>

        <!-- MongoDB Activity Log Stream -->
        <section class="glass-panel log-section">
          <div class="section-header">
            <h2 class="section-title">MongoDB Activity Logs Stream</h2>
            <button class="btn-refresh" (click)="loadData()">Refresh Logs</button>
          </div>

          <div class="table-container">
            <table class="log-table">
              <thead>
                <tr>
                  <th>Event Type</th>
                  <th>Description</th>
                  <th>User</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let log of data?.recentLogs">
                  <td>
                    <span class="badge badge-purple">{{ log.eventType }}</span>
                  </td>
                  <td class="log-desc">{{ log.description }}</td>
                  <td>{{ log.username }}</td>
                  <td class="log-time">{{ log.createdAt | date:'shortTime' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  `,
  styles: [`
    .admin-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 24px;
    }

    .header {
      padding: 16px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .logo-area {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .logo-icon {
      width: 44px;
      height: 44px;
      background: linear-gradient(135deg, #6366f1 0%, #a855f7 100%);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 1.2rem;
      color: #fff;
    }

    .logo-title {
      font-size: 1.25rem;
      font-weight: 700;
    }

    .logo-sub {
      font-size: 0.85rem;
      color: var(--text-muted);
    }

    .header-status {
      display: flex;
      gap: 12px;
    }

    .pulse-dot {
      width: 8px;
      height: 8px;
      background-color: #34d399;
      border-radius: 50%;
      box-shadow: 0 0 8px #34d399;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 20px;
    }

    .stat-card {
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .stat-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .stat-title {
      font-size: 0.9rem;
      color: var(--text-muted);
      font-weight: 500;
    }

    .stat-value {
      font-size: 2.2rem;
      font-weight: 700;
      color: var(--text-main);
    }

    .stat-desc {
      font-size: 0.8rem;
      color: var(--text-sub);
    }

    .arch-section, .log-section {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .section-title {
      font-size: 1.1rem;
      font-weight: 600;
    }

    .services-list {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
    }

    .service-chip {
      padding: 8px 16px;
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      font-size: 0.85rem;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .chip-dot {
      width: 6px;
      height: 6px;
      background-color: #818cf8;
      border-radius: 50%;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .btn-refresh {
      background: rgba(99, 102, 241, 0.2);
      color: #818cf8;
      border: 1px solid rgba(99, 102, 241, 0.4);
      padding: 6px 14px;
      border-radius: 8px;
      cursor: pointer;
      font-weight: 600;
      font-size: 0.85rem;
      transition: all 0.2s ease;
    }

    .btn-refresh:hover {
      background: rgba(99, 102, 241, 0.4);
    }

    .table-container {
      overflow-x: auto;
    }

    .log-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
      font-size: 0.9rem;
    }

    .log-table th {
      padding: 12px;
      color: var(--text-muted);
      font-weight: 600;
      border-bottom: 1px solid var(--border-color);
    }

    .log-table td {
      padding: 14px 12px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.04);
    }

    .log-desc {
      color: var(--text-main);
    }

    .log-time {
      color: var(--text-sub);
    }
  `]
})
export class AppComponent implements OnInit {
  data: AnalyticsOverview | null = null;

  constructor(private analyticsService: AnalyticsService) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.analyticsService.getOverview().subscribe({
      next: (res) => {
        this.data = res;
      },
      error: (err) => {
        console.error('Failed to load analytics data', err);
      }
    });
  }
}
