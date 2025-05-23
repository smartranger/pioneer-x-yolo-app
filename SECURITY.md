# 安全指南

## 敏感信息处理

本项目包含以下敏感信息，在提交代码或公开项目时应注意保护：

1. **密钥库文件（Keystore）**
   - 所有 `.keystore` 和 `.jks` 文件不应提交到版本控制
   - 这些文件应保存在安全的地方，如密码管理器或加密存储

2. **本地配置文件**
   - `local.properties` 包含SDK路径和用户特定信息，不应提交
   - 其他包含密码、API密钥的配置文件也应排除在版本控制之外

3. **默认凭据**
   - 避免在代码中硬编码默认密码，如 `keystorePassword = "123456"`
   - 考虑使用环境变量或安全的配置文件管理这些信息

4. **操作系统特定文件**
   - `.DS_Store`（macOS）和 `Thumbs.db`（Windows）等系统生成的文件不应提交

## 克隆项目后的安全设置

新克隆项目的开发者需要：

1. 创建自己的 `app.keystore` 或获取团队的官方密钥
2. 在本地配置 `local.properties` 文件指向自己的SDK路径
3. 检查 `.gitignore` 是否正确排除了敏感文件

## 签名密钥最佳实践

- 使用强密码保护密钥库
- 定期轮换密钥（如果适用）
- 限制密钥的访问权限
- 考虑使用CI/CD流水线中的密钥管理服务

如果您发现任何安全漏洞，请私下联系项目维护者，而不要创建公开的Issue。 