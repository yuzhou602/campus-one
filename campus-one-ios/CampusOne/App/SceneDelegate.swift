import UIKit

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(_ scene: UIScene,
               willConnectTo session: UISceneSession,
               options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = scene as? UIWindowScene else { return }
        let window = UIWindow(windowScene: windowScene)
        self.window = window
        window.rootViewController = AppRouter.shared.rootController()
        window.makeKeyAndVisible()
    }

    func sceneDidEnterBackground(_ scene: UIScene) {}
    func sceneDidBecomeActive(_ scene: UIScene) {}
}

/// 入口路由：按登录态决定登录页或主容器
enum AppRouter {
    static let shared = AppRouter()

    func rootController() -> UIViewController {
        if AuthStore.shared.isLoggedIn {
            return MainTabController()
        }
        return UINavigationController(rootViewController: LoginController())
    }

    func switchToMain() {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = scene.windows.first else { return }
        UIView.transition(with: window, duration: 0.28, options: .transitionCrossDissolve) {
            window.rootViewController = MainTabController()
        }
    }

    func switchToLogin() {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = scene.windows.first else { return }
        AuthStore.shared.clear()
        UIView.transition(with: window, duration: 0.28, options: .transitionCrossDissolve) {
            window.rootViewController = UINavigationController(rootViewController: LoginController())
        }
    }
}