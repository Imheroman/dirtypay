import axios, { AxiosError } from "axios";
import { toast } from "sonner";

/**
 * API 클라이언트
 * - BFF 패턴: /api/proxy를 통해 백엔드 API 호출
 * - 토큰 관리: 서버 사이드 iron-session에서 처리
 * - 401 에러 시 자동 토큰 갱신은 proxy에서 처리
 */
export const apiClient = axios.create({
  baseURL: "/api/proxy",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

// 중복 redirect 방지
let isRedirecting = false;

// Response Interceptor - 에러 처리
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error: AxiosError) => {
    if (error.response) {
      const status = error.response.status;

      switch (status) {
        case 401:
          if (typeof window !== "undefined" && !isRedirecting) {
            isRedirecting = true;
            alert("세션이 만료되었어요. 다시 로그인해 주세요.");
            fetch("/api/auth/logout", { method: "POST" }).finally(() => {
              window.location.href = "/login";
            });
          }
          break;
        case 403:
          if (typeof window !== "undefined") {
            alert("접근 권한이 없어요.");
            window.history.back();
          }
          break;
        case 404:
          console.error("Not Found - 요청한 정보를 찾을 수 없어요.");
          break;
        case 409:
          // 409는 비즈니스 로직 에러이므로 각 mutation의 onError에서 개별 처리
          break;
        case 500:
          toast.error("서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요.");
          break;
        default:
          console.error(`Error: ${status}`);
      }
    } else if (error.request) {
      toast.error("네트워크 연결을 확인해 주세요.");
    }

    return Promise.reject(error);
  }
);

export default apiClient;
