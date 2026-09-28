import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface StatsResponse<T> {
  data: T;
  meta: { source: string; lastSynced: string | null };
}

export interface ChartData {
  labels: string[];
  datasets: { label: string; data: number[] }[];
}

@Injectable({ providedIn: 'root' })
export class StatsService {
  private readonly api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getOverview(): Observable<StatsResponse<Record<string, Record<string, number>>>> {
    return this.http.get<StatsResponse<any>>(`${this.api}/api/stats/overview`);
  }

  getByCondition(condition = 'Depression'): Observable<StatsResponse<ChartData>> {
    return this.http.get<StatsResponse<ChartData>>(
      `${this.api}/api/stats/by-condition`,
      { params: { condition } }
    );
  }

  getByState(year?: number): Observable<StatsResponse<Record<string, Record<string, number>>>> {
    const params: Record<string, string> = {};
    if (year) params['year'] = String(year);
    return this.http.get<StatsResponse<any>>(`${this.api}/api/stats/by-state`, { params });
  }

  getFullDataset(page = 0, size = 50): Observable<StatsResponse<any>> {
    return this.http.get<StatsResponse<any>>(
      `${this.api}/api/stats/advanced/full`,
      { params: { page: String(page), size: String(size) } }
    );
  }
}
