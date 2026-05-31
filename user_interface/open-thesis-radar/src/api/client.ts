export class ApiError extends Error {
  readonly status: number;
  readonly payload: unknown;

  constructor(message: string, status: number, payload: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

const DEFAULT_BASE_URL = '';

export class ApiClient {
  private readonly baseUrl: string;

  constructor(baseUrl: string = DEFAULT_BASE_URL) {
    this.baseUrl = baseUrl;
  }

  async request<TResponse>(path: string, init?: RequestInit): Promise<TResponse> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(init?.headers ?? {}),
      },
    });

    if (!response.ok) {
      const payload = await this.safeParseJson(response);
      const message = this.getErrorMessage(payload, response.status);
      throw new ApiError(message, response.status, payload);
    }

    if (response.status === 204) {
      return undefined as TResponse;
    }

    return (await this.parseJsonResponse(response)) as TResponse;
  }

  private async safeParseJson(response: Response): Promise<unknown> {
    try {
      return await response.json();
    } catch {
      return null;
    }
  }

  private getErrorMessage(payload: unknown, status: number): string {
    if (
      payload &&
      typeof payload === 'object' &&
      'message' in payload &&
      typeof (payload as { message?: unknown }).message === 'string'
    ) {
      return (payload as { message: string }).message;
    }

    return `Request failed with status ${status}`;
  }

  private async parseJsonResponse(response: Response): Promise<unknown> {
    try {
      return await response.json();
    } catch {
      throw new ApiError(
        'Expected JSON from API, but received a non-JSON response. Check that the backend or Vite proxy is running for /api requests.',
        response.status,
        null,
      );
    }
  }
}

export const defaultApiClient = new ApiClient();
