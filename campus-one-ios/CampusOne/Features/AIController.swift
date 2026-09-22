import UIKit

/// AI 校园助手（对应 Vue AICopilot）。消息气泡 + 快捷指令 + 输入栏。
final class AIController: UIViewController {
    private struct Bail: Decodable {
        let content: String?
        let conversationId: String?
        let sources: [String]?
    }
    private struct Message {
        let role: String // "user" / "assistant"
        let content: String
        var sources: [String] = []
    }
    private struct ChatBody: Encodable {
        let message: String
        let conversationId: String
    }

    private var messages: [Message] = []
    private var conversationId = ""
    private var sending = false

    private let chatScroll = UIScrollView()
    private let messagesStack = UIStackView()
    private let inputField = InkTextField("输入你想了解或办理的校园事务……")
    private let sendButton = PrimaryButton("发送")
    private let quickActions = ["查课表", "找教室", "查申请", "校园规定", "场地预约", "报修进度", "校园活动"]

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "AI 校园助手"
        view.backgroundColor = Theme.paper
        sendButton.heightAnchor.constraint(equalToConstant: 50).isActive = true
        sendButton.addTarget(self, action: #selector(send), for: .touchUpInside)
        buildUI()
    }

    private func buildUI() {
        // 页眉 + 快捷指令
        let head = ArchiveHeader()
        head.titleLabel.text = "Campus Copilot"
        head.subtitleLabel.text = "你的 AI 校园生活与事务助手"
        head.markLabel.text = "AI · 智档"

        let quick = UIStackView()
        quick.axis = .horizontal
        quick.spacing = 8
        quick.distribution = .fillEqually
        // 两行快捷指令（横排过多会溢出，仅展示前 4 个）
        let shown = Array(quickActions.prefix(4))
        for (i, act) in shown.enumerated() {
            let chip = quickChip(String(format: "%02d", i + 1), act)
            quick.addArrangedSubview(chip)
        }

        // 消息区
        chatScroll.translatesAutoresizingMaskIntoConstraints = false
        messagesStack.axis = .vertical
        messagesStack.spacing = 12
        messagesStack.translatesAutoresizingMaskIntoConstraints = false
        chatScroll.addSubview(messagesStack)

        // 初始问候
        let greeting = Message(role: "assistant", content: "输入你想了解或办理的校园事务……")
        insertBubble(greeting, animated: false)

        // 输入区
        let inputCard = ArchiveCard()
        let inputRow = UIStackView(arrangedSubviews: [inputField, sendButton])
        inputRow.axis = .horizontal; inputRow.spacing = 10; inputRow.alignment = .center
        inputRow.translatesAutoresizingMaskIntoConstraints = false
        inputCard.addSubview(inputRow)
        [inputCard, head, quick].forEach { $0.translatesAutoresizingMaskIntoConstraints = false }

        view.addSubview(head)
        view.addSubview(quick)
        view.addSubview(chatScroll)
        view.addSubview(inputCard)

        NSLayoutConstraint.activate([
            head.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 16),
            head.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            head.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),

            quick.topAnchor.constraint(equalTo: head.bottomAnchor, constant: 14),
            quick.leadingAnchor.constraint(equalTo: head.leadingAnchor),
            quick.trailingAnchor.constraint(equalTo: head.trailingAnchor),
            quick.heightAnchor.constraint(equalToConstant: 34),

            chatScroll.topAnchor.constraint(equalTo: quick.bottomAnchor, constant: 14),
            chatScroll.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            chatScroll.trailingAnchor.constraint(equalTo: view.trailingAnchor),

            inputCard.topAnchor.constraint(equalTo: chatScroll.bottomAnchor, constant: 10),
            inputCard.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            inputCard.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            inputCard.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -12),

            inputRow.leadingAnchor.constraint(equalTo: inputCard.leadingAnchor, constant: 12),
            inputRow.trailingAnchor.constraint(equalTo: inputCard.trailingAnchor, constant: -12),
            inputRow.topAnchor.constraint(equalTo: inputCard.topAnchor, constant: 12),
            inputRow.bottomAnchor.constraint(equalTo: inputCard.bottomAnchor, constant: -12),

            messagesStack.leadingAnchor.constraint(equalTo: chatScroll.leadingAnchor, constant: 16),
            messagesStack.trailingAnchor.constraint(equalTo: chatScroll.trailingAnchor, constant: -16),
            messagesStack.topAnchor.constraint(equalTo: chatScroll.topAnchor, constant: 8),
            messagesStack.bottomAnchor.constraint(equalTo: chatScroll.bottomAnchor, constant: -8),
            messagesStack.widthAnchor.constraint(equalTo: chatScroll.widthAnchor, constant: -32),
        ])
    }

    private func quickChip(_ no: String, _ title: String) -> UIButton {
        let b = UIButton(type: .system)
        b.backgroundColor = Theme.surface
        b.layer.borderWidth = 1
        b.layer.borderColor = Theme.line.cgColor
        b.layer.cornerRadius = 8
        let t = UILabel(); t.text = title; t.font = .systemFont(ofSize: 12); t.textColor = Theme.ink700
        let n = MonoLabel(no, size: 9)
        let row = UIStackView(arrangedSubviews: [n, t]); row.spacing = 6; row.alignment = .center
        row.isUserInteractionEnabled = false
        b.addSubview(row); row.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            row.centerXAnchor.constraint(equalTo: b.centerXAnchor),
            row.centerYAnchor.constraint(equalTo: b.centerYAnchor),
        ])
        b.addAction(UIAction { [weak self] _ in self?.sendText(title) }, for: .touchUpInside)
        return b
    }

    private func bubble(_ msg: Message) -> UIView {
        let isUser = msg.role == "user"
        let container = ArchiveCard()
        container.layer.cornerRadius = 12
        if isUser {
            container.backgroundColor = Theme.ink900
            container.layer.borderColor = Theme.ink900.cgColor
        }
        let label = UILabel()
        label.text = msg.content
        label.font = .systemFont(ofSize: 14)
        label.textColor = isUser ? .white : Theme.ink900
        label.numberOfLines = 0

        var arranged: [UIView] = [label]
        if !isUser, !msg.sources.isEmpty {
            let srcTitle = UILabel(); srcTitle.text = "参考资料"; srcTitle.font = .mono(10); srcTitle.textColor = Theme.ink300
            let sep = UIView(); sep.backgroundColor = Theme.line; sep.heightAnchor.constraint(equalToConstant: 1).isActive = true
            var srcViews: [UIView] = [srcTitle, sep]
            for (i, s) in msg.sources.enumerated() {
                let row = UILabel()
                row.text = String(format: "REF·%d  %@", i + 1, s)
                row.font = .mono(11); row.textColor = Theme.ink700; row.numberOfLines = 0
                srcViews.append(row)
            }
            let src = UIStackView(arrangedSubviews: srcViews)
            src.axis = .vertical; src.spacing = 4; src.alignment = .leading
            arranged.append(src)
        }

        let v = UIStackView(arrangedSubviews: arranged)
        v.axis = .vertical; v.spacing = 8
        v.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(v)
        NSLayoutConstraint.activate([
            v.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 12),
            v.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -12),
            v.topAnchor.constraint(equalTo: container.topAnchor, constant: 12),
            v.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -12),
        ])
        container.widthAnchor.constraint(lessThanOrEqualToConstant: 300).isActive = true
        return container
    }

    private func insertBubble(_ msg: Message, animated: Bool) {
        let isUser = msg.role == "user"
        let bubbleView = bubble(msg)
        let spacer = UIView()
        spacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        let row = UIStackView(arrangedSubviews: isUser ? [spacer, bubbleView] : [bubbleView, spacer])
        row.alignment = .bottom
        row.spacing = 4
        messagesStack.addArrangedSubview(row)
        if animated {
            row.alpha = 0
            UIView.animate(withDuration: 0.2) { row.alpha = 1 }
        }
        scrollToBottom()
    }

    private func scrollToBottom() {
        let bottom = CGPoint(x: 0, y: max(chatScroll.contentSize.height - chatScroll.bounds.height, 0))
        chatScroll.setContentOffset(bottom, animated: true)
    }

    @objc private func send() {
        sendText(inputField.text ?? "")
    }

    private func sendText(_ text: String) {
        let content = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !content.isEmpty, !sending else { return }
        sending = true
        inputField.text = ""
        inputField.resignFirstResponder()
        let userMsg = Message(role: "user", content: content)
        messages.append(userMsg)
        insertBubble(userMsg, animated: true)

        let body = ChatBody(message: content, conversationId: conversationId)
        Task {
            do {
                let res: Bail = try await APIClient.shared.request("POST", "ai/chat", body: body)
                await MainActor.run {
                    self.conversationId = res.conversationId ?? ""
                    if let c = res.content, !c.isEmpty {
                        let a = Message(role: "assistant", content: c, sources: res.sources ?? [])
                        self.messages.append(a)
                        self.insertBubble(a, animated: true)
                    }
                    self.sending = false
                }
            } catch {
                await MainActor.run {
                    let fallback = Message(role: "assistant", content: "抱歉，AI 服务暂时不可用。请稍后再试。")
                    self.messages.append(fallback)
                    self.insertBubble(fallback, animated: true)
                    self.sending = false
                }
            }
        }
    }
}