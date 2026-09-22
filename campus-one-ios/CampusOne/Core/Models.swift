import Foundation
import Security

// —— 会话持久化 ——
final class AuthStore {
    static let shared = AuthStore()
    private let defaults = UserDefaults.standard
    private let keychainService = "com.campusone.app.auth"

    var token: String? {
        get { secureValue(for: "accessToken") }
        set { setSecureValue(newValue, for: "accessToken") }
    }
    var refreshToken: String? {
        get { secureValue(for: "refreshToken") }
        set { setSecureValue(newValue, for: "refreshToken") }
    }
    var username: String {
        get { defaults.string(forKey: "username") ?? "" }
        set { defaults.set(newValue, forKey: "username") }
    }
    var realName: String {
        get { defaults.string(forKey: "realName") ?? "" }
        set { defaults.set(newValue, forKey: "realName") }
    }
    var role: String {
        get { defaults.string(forKey: "role") ?? "" }
        set { defaults.set(newValue, forKey: "role") }
    }

    var isLoggedIn: Bool { !(token ?? "").isEmpty }

    var isAdmin: Bool { ["ADMIN", "SUPER_ADMIN"].contains(role) }
    var isApprover: Bool { ["TEACHER", "COUNSELOR", "ADMIN", "SUPER_ADMIN"].contains(role) }

    var roleText: String {
        switch role {
        case "SUPER_ADMIN": return "超级管理员"
        case "ADMIN": return "管理员"
        case "TEACHER": return "教师"
        case "COUNSELOR": return "职工"
        case "STUDENT": return "学生"
        default: return "成员"
        }
    }

    func save(token: String, refreshToken: String?, username: String, realName: String, role: String) {
        self.token = token
        self.refreshToken = refreshToken
        self.username = username
        self.realName = realName
        self.role = role
    }
    func clear() {
        token = nil; refreshToken = nil; username = ""; realName = ""; role = ""
    }

    private func secureValue(for account: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private func setSecureValue(_ value: String?, for account: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: account
        ]
        SecItemDelete(query as CFDictionary)
        guard let value, let data = value.data(using: .utf8) else { return }
        var attributes = query
        attributes[kSecValueData as String] = data
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(attributes as CFDictionary, nil)
    }
}

// —— 领域模型 ——
struct LoginRequest: Encodable { let username: String; let password: String }
struct LoginResponse: Decodable {
    let accessToken: String
    let refreshToken: String?
    let expiresIn: Int?
    let userInfo: UserProfile
    struct UserProfile: Decodable {
        let id: Int
        let username: String
        let realName: String
        let role: String
        let avatar: String?
    }
}

struct UserItem: Decodable, Hashable {
    let id: Int
    let username: String
    let realName: String
    let role: String
    let email: String?
    let phone: String?
    let status: Int
}

// —— 数据中心 / 仪表盘宏观指标 ——
struct AnalyticsOverview: Decodable {
    let totalUsers: Int?
    let todayReservations: Int?
    let pendingRepairs: Int?
    let totalActivities: Int?
}

// —— 系统运行信息 ——
struct SystemInfo: Decodable {
    let name: String?
    let version: String?
    let javaVersion: String?
    let osName: String?
}

// —— 今日课程（学生仪表盘）——
struct CVClassItem: Decodable {
    let id: Int
    let name: String?
    let startTime: String?
    let endTime: String?
    let location: String?
    let teacher: String?
    let week: Int?
}

struct StudentDashboard: Decodable {
    let recentActivities: [CVClassItem]?
    let pendingRepairs: Int?
    let upcomingReservations: Int?
}

// 教师仪表盘（暂与学生同构，字段可少）
struct TeacherDashboard: Decodable {
    let pendingRepairs: Int?
    let upcomingReservations: Int?
}

// —— 校园通知 ——
struct NoticeItem: Decodable {
    let id: Int
    let title: String?
    let createdAt: String?
    let isRead: Bool?
}

// —— 校园活动 ——
struct ActivityItem: Decodable {
    let id: Int
    let title: String?
    let startTime: String?
    let location: String?
    let registeredCount: Int?
    let capacity: Int?
}

// —— 场地 / 报修分析 ——
struct VenueAnalytics: Decodable {
    let totalReservations: Int?
    let todayReservations: Int?
    let avgDaily: Int?
}

struct RepairAnalytics: Decodable {
    let accepted: Int?
    let submitted: Int?
    let resolved: Int?
}

// —— 审批条目（帖 / 处理 / 发起共用）——
struct ApprovalItem: Decodable {
    let id: Int
    let applicationNo: String?
    let applicantId: Int?
    let applicantName: String?
    let currentNode: String?
    let status: String?
}

// —— 审批动作请求体 ——
struct ApprovalActionRequest: Encodable {
    let action: String
    let comment: String
}
