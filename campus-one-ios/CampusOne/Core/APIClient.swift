import Foundation

/// 后端统一入口，路由一律 /api/v1 前缀
enum API {
    static let base: URL = {
        guard let value = Bundle.main.object(forInfoDictionaryKey: "APIBaseURL") as? String,
              let url = URL(string: value), !value.isEmpty else {
            preconditionFailure("请在构建设置中配置 API_BASE_URL")
        }
        return url
    }()
    static var token: String? = AuthStore.shared.token

    static func path(_ p: String) -> URL { base.appendingPathComponent(p) }
}

struct APIResult<T: Decodable>: Decodable {
    let code: Int
    let message: String?
    let data: T?
    let success: Bool?
}

private struct EmptyResponse: Decodable {}

struct Page<T: Decodable>: Decodable {
    let records: [T]?
    let total: Int?
    let current: Int?
    let size: Int?
}

enum APIError: LocalizedError {
    case message(String)
    var errorDescription: String? {
        switch self {
        case .message(let m): return m
        }
    }
}

actor APIClient {
    static let shared = APIClient()

    func request<T: Decodable>(_ method: String, _ path: String,
                               query: [String: String] = [:],
                               body: Encodable? = nil) async throws -> T {
        var url = API.path(path)
        if !query.isEmpty {
            var comps = URLComponents(url: url, resolvingAgainstBaseURL: false)!
            comps.queryItems = query.map { URLQueryItem(name: $0.key, value: $0.value) }
            url = comps.url!
        }

        var req = URLRequest(url: url)
        req.httpMethod = method
        req.timeoutInterval = 20
        if let token = AuthStore.shared.token {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        if let body {
            req.httpBody = try JSONEncoder().encode(body)
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        let (data, resp) = try await URLSession.shared.data(for: req)
        guard let http = resp as? HTTPURLResponse else { throw APIError.message("网络异常") }
        let obj = try? JSONDecoder().decode(APIResult<T>.self, from: data)
        guard (200..<300).contains(http.statusCode) else {
            throw APIError.message(obj?.message ?? "请求失败(\(http.statusCode))")
        }
        guard let obj else { throw APIError.message("数据解析失败") }
        if obj.success == false || obj.code >= 400 {
            throw APIError.message(obj.message ?? "请求失败")
        }
        guard let result = obj.data else { throw APIError.message("响应缺少数据") }
        return result
    }

    func requestVoid(_ method: String, _ path: String, body: Encodable? = nil) async throws {
        var url = API.path(path)
        var req = URLRequest(url: url)
        req.httpMethod = method
        if let token = AuthStore.shared.token { req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        if let body {
            req.httpBody = try JSONEncoder().encode(body)
            req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        let (data, resp) = try await URLSession.shared.data(for: req)
        guard let http = resp as? HTTPURLResponse else { throw APIError.message("网络异常") }
        let obj = try? JSONDecoder().decode(APIResult<EmptyResponse>.self, from: data)
        guard (200..<300).contains(http.statusCode), obj?.success != false, (obj?.code ?? 200) < 400 else {
            throw APIError.message(obj?.message ?? "请求失败(\(http.statusCode))")
        }
    }
}
