package com.example.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import com.example.data.repo.AuraRepository
import com.example.service.*
import com.example.webview.*
import com.example.work.*
import com.example.token.TokenManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

data class AgentUpgradeConfig(
    val customWelcome: String = "星空浩瀚，鹏鲲展翅，AI进化无边界。",
    val quantumCoreActive: Boolean = false,
    val sparkBoostMultiplier: Int = 1,
    val dynamicThemeColor: String = "SPACE_DEFAULT",
    val installedUpgrades: List<String> = emptyList()
)

class AuraViewModel(
    private val repository: AuraRepository,
    private val filesDir: File,
    private val context: Context
) : ViewModel() {

    // ==================== 真实引擎 ====================
    private val gatewayServer = LocalOpenAiServer(context, 8000)
    private val keyPoolManager = KeyPoolManager(repository.database)
    private val tempMailClient = TempMailClient()
    private val webViewEngine = WebViewRegistrationEngine()
    private val kiroEngine = KiroProtocolEngine()
    private val cerebrasEngine = CerebrasProtocolEngine()
    private val traeEngine = TraeProtocolEngine()
    
    // SMS 接码服务（需要用户配置 API Key）
    private var smsService: com.example.service.SmsService? = null
    
    // 需要 SMS 的平台引擎
    private var chatGptEngine: ChatGptEngine? = null
    private var claudeEngine: ClaudeEngine? = null
    
    // 邮箱注册平台引擎
    private val deepSeekEngine = DeepSeekEngine()
    private val groqEngine = GroqEngine()
    
    // Token 管理器
    val tokenManager = TokenManager(context)

    // ==================== SMS 接码服务配置 ====================
    private val _smsProvider = MutableStateFlow("sms-activate")
    val smsProvider: StateFlow<String> = _smsProvider.asStateFlow()
    
    private val _smsApiKey = MutableStateFlow("")
    val smsApiKey: StateFlow<String> = _smsApiKey.asStateFlow()
    
    private val _smsBalance = MutableStateFlow("未检查")
    val smsBalance: StateFlow<String> = _smsBalance.asStateFlow()
    
    fun setSmsProvider(provider: String) {
        _smsProvider.value = provider
        initSmsService()
    }
    
    fun setSmsApiKey(key: String) {
        _smsApiKey.value = key
        initSmsService()
    }
    
    private fun initSmsService() {
        val key = _smsApiKey.value
        if (key.isNotEmpty()) {
            try {
                smsService = com.example.service.SmsServiceFactory.create(_smsProvider.value, key)
                chatGptEngine = ChatGptEngine(smsService!!)
                claudeEngine = ClaudeEngine(smsService!!)
            } catch (e: Exception) {
                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                    "⚠️ SMS 服务初始化失败: ${e.message}"
            }
        }
    }
    
    fun checkSmsBalance() {
        viewModelScope.launch(Dispatchers.IO) {
            val service = smsService
            if (service == null) {
                _smsBalance.value = "未配置"
                return@launch
            }
            val balance = service.getBalance()
            _smsBalance.value = if (balance >= 0) "$${String.format("%.2f", balance)}" else "查询失败"
        }
    }
    
    // ==================== CF Workers 配置 ====================
    private val _cfWorkersList = MutableStateFlow<List<String>>(listOf(
        "https://proxy.pengkun.workers.dev",
        "https://cf-openai-switch.ai.workers.dev",
        "https://api-gateway-backup.workers.dev"
    ))
    val cfWorkersList: StateFlow<List<String>> = _cfWorkersList.asStateFlow()

    private val _selectedCfWorker = MutableStateFlow("https://proxy.pengkun.workers.dev")
    val selectedCfWorker: StateFlow<String> = _selectedCfWorker.asStateFlow()

    private val _cfProxyEnabled = MutableStateFlow(true)
    val cfProxyEnabled: StateFlow<Boolean> = _cfProxyEnabled.asStateFlow()

    private val _cfLatencyList = MutableStateFlow<Map<String, String>>(emptyMap())
    val cfLatencyList: StateFlow<Map<String, String>> = _cfLatencyList.asStateFlow()

    fun addCfWorker(url: String) {
        val trimmed = url.trim()
        if (trimmed.isNotEmpty() && !_cfWorkersList.value.contains(trimmed)) {
            _cfWorkersList.value = _cfWorkersList.value + trimmed
            // 存入数据库
            viewModelScope.launch {
                repository.database.cfWorkerConfigDao().insertWorker(
                    CfWorkerConfig(url = trimmed)
                )
            }
        }
    }

    fun deleteCfWorker(url: String) {
        _cfWorkersList.value = _cfWorkersList.value - url
    }

    fun selectCfWorker(url: String) {
        _selectedCfWorker.value = url
    }

    fun toggleCfProxy(enabled: Boolean) {
        _cfProxyEnabled.value = enabled
    }

    fun testCfWorkers() {
        viewModelScope.launch(Dispatchers.IO) {
            val results = mutableMapOf<String, String>()
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            _cfWorkersList.value.forEach { worker ->
                try {
                    val start = System.currentTimeMillis()
                    val resp = client.newCall(
                        okhttp3.Request.Builder().url(worker).get().build()
                    ).execute()
                    val latency = System.currentTimeMillis() - start
                    results[worker] = if (resp.isSuccessful) "${latency}ms" else "错误 ${resp.code}"
                } catch (e: Exception) {
                    results[worker] = "超时/不可达"
                }
            }
            _cfLatencyList.value = results
        }
    }

    // ==================== 模型选择配置 ====================
    private val _selectedModel = MutableStateFlow("auto")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedModelTier = MutableStateFlow("all")
    val selectedModelTier: StateFlow<String> = _selectedModelTier.asStateFlow()

    val availableModels: List<com.example.service.ModelEntry> get() = com.example.service.MODEL_CATALOG

    fun selectModel(modelId: String) { _selectedModel.value = modelId }
    fun selectModelTier(tier: String) { _selectedModelTier.value = tier }

    // ==================== 密钥池配置 ====================
    private val _minKeyReserveThreshold = MutableStateFlow(5)
    val minKeyReserveThreshold: StateFlow<Int> = _minKeyReserveThreshold.asStateFlow()

    fun setMinKeyReserve(value: Int) {
        _minKeyReserveThreshold.value = value
    }

    // ==================== 自动注册 ====================
    private val _autoRegRunning = MutableStateFlow(false)
    val autoRegRunning: StateFlow<Boolean> = _autoRegRunning.asStateFlow()

    private val _selectedAutoRegProviders = MutableStateFlow(listOf("Kiro", "Cerebras", "Trae", "DeepSeek", "Groq"))
    val selectedAutoRegProviders: StateFlow<List<String>> = _selectedAutoRegProviders.asStateFlow()

    private val _tempMailInboxLogs = MutableStateFlow<List<String>>(listOf(
        "🟢 常驻后台守护进程就绪.",
        "🟢 临时邮箱连接池已建立 (mail.gw API)",
        "🟢 WebView 注册引擎已初始化",
        "🟢 Kiro 协议引擎已就绪"
    ))
    val tempMailInboxLogs: StateFlow<List<String>> = _tempMailInboxLogs.asStateFlow()

    private val _selectedFingerprintMode = MutableStateFlow("Android/WebView (Randomized UA + Isolated Sandbox)")
    val selectedFingerprintMode: StateFlow<String> = _selectedFingerprintMode.asStateFlow()

    private val _accountSalts = MutableStateFlow(0)
    val accountSalts: StateFlow<Int> = _accountSalts.asStateFlow()

    fun toggleAutoRegProvider(provider: String) {
        val currentList = _selectedAutoRegProviders.value
        _selectedAutoRegProviders.value = if (currentList.contains(provider)) {
            currentList - provider
        } else {
            currentList + provider
        }
    }

    fun toggleAutoReg() {
        val current = _autoRegRunning.value
        _autoRegRunning.value = !current

        if (!current) {
            val selected = _selectedAutoRegProviders.value
            if (selected.isEmpty()) {
                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                    "⚠️ 错误: 请至少勾选一个 AI 平台"
                _autoRegRunning.value = false
                return
            }

            _tempMailInboxLogs.value = _tempMailInboxLogs.value + listOf(
                "🚀 [AutoReg] 启动全自动注册线程，目标平台: ${selected.joinToString()}",
                "🎭 [沙箱] 独立 WebView 缓存空间已分配",
                "🎭 [UA] 动态设备指纹: ${WebViewRegistrationEngine.USER_AGENTS.random()}"
            )

            viewModelScope.launch(Dispatchers.IO) {
                while (_autoRegRunning.value && selected.isNotEmpty()) {
                    val provider = selected.random()
                    _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                        "🎯 [$provider] 开始注册流程..."

                    // 获取临时邮箱
                    delay(2000)
                    val mailAccount = tempMailClient.createAccount()
                    if (mailAccount == null) {
                        _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                            "❌ [$provider] 无法创建临时邮箱，跳过..."
                        delay(10000)
                        continue
                    }

                    _tempMailInboxLogs.value = _tempMailInboxLogs.value + listOf(
                        "📬 临时邮箱创建成功: ${mailAccount.email}"
                    )

                    // 记录注册任务
                    repository.database.registrationTaskDao().insertTask(
                        RegistrationTask(
                            provider = provider,
                            status = "running",
                            accountEmail = mailAccount.email,
                            tempMailId = mailAccount.mailId
                        )
                    )

                    when (provider) {
                        "Kiro" -> {
                            // ===== Kiro 协议注册 =====
                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "🔐 [Kiro] 使用协议模式 (AWS Builder ID)"

                            val regResult = kiroEngine.register(mailAccount.email)
                            if (!regResult.success) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [Kiro] 注册失败: ${regResult.error}"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "⏳ [Kiro] OTP已发送，等待验证码..."

                            val code = tempMailClient.waitForVerificationCode(
                                mailAccount, 120000L
                            )
                            if (code == null) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "⚠️ [Kiro] 未收到验证码，跳过"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "📨 [Kiro] 收到验证码: $code"

                            val tokens = kiroEngine.completeWithOtp(
                                code, mailAccount.email,
                                regResult.password
                            )
                            if (tokens.success) {
                                val accessToken = tokens.accessToken ?: ""
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value + listOf(
                                    "🎉 [Kiro] 注册成功!",
                                    "📧 $mailAccount.email",
                                    "🔑 accessToken: ${accessToken.take(20)}..."
                                )
                                // 存入密钥池
                                repository.insertKey(ApiKeyItem(
                                    keyLabel = "Kiro 自动注册",
                                    keyValue = accessToken,
                                    provider = "Kiro",
                                    status = "Active"
                                ))
                            } else {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [Kiro] OTP验证失败: ${tokens.error}"
                            }
                        }
                        "Cerebras" -> {
                            // ===== Cerebras 协议注册 (Stytch OTP) =====
                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "🔐 [$provider] 使用协议模式 (Stytch OTP)"

                            val regResult = cerebrasEngine.register(mailAccount.email)
                            if (!regResult.success) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 注册失败: ${regResult.error}"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "⏳ [$provider] OTP已发送，等待验证码..."

                            val code = tempMailClient.waitForVerificationCode(
                                mailAccount, 120000L
                            )
                            if (code == null) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "⚠️ [$provider] 未收到验证码，跳过"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "📨 [$provider] 收到验证码: $code"

                            val result = cerebrasEngine.completeWithOtp(
                                mailAccount.email, code
                            )
                            if (result.success) {
                                val apiKey = result.apiKey ?: ""
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value + listOf(
                                    "🎉 [$provider] 注册成功!",
                                    "📧 ${mailAccount.email}",
                                    "🔑 API Key: ${apiKey.take(20)}..."
                                )
                                repository.insertKey(ApiKeyItem(
                                    keyLabel = "$provider 自动注册",
                                    keyValue = apiKey,
                                    provider = provider,
                                    status = "Active"
                                ))
                            } else {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 验证失败: ${result.error}"
                            }
                        }

                        "Trae" -> {
                            // ===== Trae 协议注册 (TikTok passport) =====
                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "🔐 [$provider] 使用协议模式 (TikTok passport)"

                            val regResult = traeEngine.register(mailAccount.email)
                            if (!regResult.success) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 注册失败: ${regResult.error}"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "⏳ [$provider] OTP已发送，等待验证码..."

                            val code = tempMailClient.waitForVerificationCode(
                                mailAccount, 120000L
                            )
                            if (code == null) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "⚠️ [$provider] 未收到验证码，跳过"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "📨 [$provider] 收到验证码: $code"

                            val result = traeEngine.completeWithOtp(
                                mailAccount.email, code
                            )
                            if (result.success) {
                                val token = result.token ?: ""
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value + listOf(
                                    "🎉 [$provider] 注册成功!",
                                    "📧 ${mailAccount.email}",
                                    "🔑 Token: ${token.take(20)}..."
                                )
                                repository.insertKey(ApiKeyItem(
                                    keyLabel = "$provider 自动注册",
                                    keyValue = token,
                                    provider = provider,
                                    status = "Active"
                                ))
                            } else {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 验证失败: ${result.error}"
                            }
                        }

                        "DeepSeek" -> {
                            // ===== DeepSeek 协议注册（邮箱即可）=====
                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "🔐 [$provider] 使用协议模式（邮箱注册）"

                            val password = generatePassword()
                            val regResult = deepSeekEngine.register(mailAccount.email, password)
                            if (!regResult.success) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 注册失败: ${regResult.error}"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "⏳ [$provider] OTP已发送，等待验证码..."

                            val code = tempMailClient.waitForVerificationCode(mailAccount, 120000L)
                            if (code == null) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "⚠️ [$provider] 未收到验证码，跳过"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "📨 [$provider] 收到验证码: $code"

                            val result = deepSeekEngine.completeWithCode(mailAccount.email, code, password)
                            if (result.success) {
                                val apiKey = result.apiKey ?: ""
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value + listOf(
                                    "🎉 [$provider] 注册成功!",
                                    "📧 ${mailAccount.email}",
                                    "🔑 API Key: ${apiKey.take(20)}..."
                                )
                                repository.insertKey(ApiKeyItem(
                                    keyLabel = "$provider 自动注册",
                                    keyValue = apiKey,
                                    provider = provider,
                                    status = "Active"
                                ))
                            } else {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 注册失败: ${result.error}"
                            }
                        }

                        "Groq" -> {
                            // ===== Groq 协议注册（邮箱即可）=====
                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "🔐 [$provider] 使用协议模式（邮箱注册）"

                            val password = generatePassword()
                            val regResult = groqEngine.register(mailAccount.email)
                            if (!regResult.success) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 注册失败: ${regResult.error}"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "⏳ [$provider] OTP已发送，等待验证码..."

                            val code = tempMailClient.waitForVerificationCode(mailAccount, 120000L)
                            if (code == null) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "⚠️ [$provider] 未收到验证码，跳过"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "📨 [$provider] 收到验证码: $code"

                            val result = groqEngine.completeWithCode(mailAccount.email, code, password)
                            if (result.success) {
                                val apiKey = result.apiKey ?: ""
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value + listOf(
                                    "🎉 [$provider] 注册成功!",
                                    "📧 ${mailAccount.email}",
                                    "🔑 API Key: ${apiKey.take(20)}..."
                                )
                                repository.insertKey(ApiKeyItem(
                                    keyLabel = "$provider 自动注册",
                                    keyValue = apiKey,
                                    provider = provider,
                                    status = "Active"
                                ))
                            } else {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 注册失败: ${result.error}"
                            }
                        }

                        "ChatGPT" -> {
                            // ===== ChatGPT 注册（需要 SMS）=====
                            val engine = chatGptEngine
                            if (engine == null) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "⚠️ [$provider] 需要配置 SMS 接码服务（设置 → SMS 配置）"
                                delay(5000)
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "🔐 [$provider] 使用协议模式（需要 SMS 验证）"

                            val password = generatePassword()
                            val regResult = engine.register(mailAccount.email, password)
                            if (!regResult.success) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 注册失败: ${regResult.error}"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "⏳ [$provider] 邮箱验证码已发送..."

                            // 等待邮箱验证码
                            val emailCode = tempMailClient.waitForVerificationCode(mailAccount, 120000L)
                            if (emailCode == null) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "⚠️ [$provider] 未收到邮箱验证码，跳过"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            if (!engine.continueWithEmailCode(mailAccount.email, emailCode)) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 邮箱验证码错误"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "📱 [$provider] 购买手机号中..."

                            // 购买手机号并发送验证码
                            if (!engine.buyAndSendPhoneCode()) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 购买手机号失败，请检查 SMS 余额"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "⏳ [$provider] 等待手机验证码..."

                            // 等待手机验证码（从 SMS 服务获取）
                            val phoneCode = smsService?.waitForCode(engine.phoneOrderId, 120000L)
                            if (phoneCode == null) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "⚠️ [$provider] 未收到手机验证码，跳过"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            val result = engine.completeWithPhoneCode(password, phoneCode)
                            if (result.success) {
                                val accessToken = result.accessToken ?: ""
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value + listOf(
                                    "🎉 [$provider] 注册成功!",
                                    "📧 ${mailAccount.email}",
                                    "🔑 Access Token: ${accessToken.take(20)}..."
                                )
                                repository.insertKey(ApiKeyItem(
                                    keyLabel = "$provider 自动注册",
                                    keyValue = accessToken,
                                    provider = provider,
                                    status = "Active"
                                ))
                            } else {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 注册失败: ${result.error}"
                            }
                        }

                        "Claude" -> {
                            // ===== Claude 注册（需要 SMS）=====
                            val engine = claudeEngine
                            if (engine == null) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "⚠️ [$provider] 需要配置 SMS 接码服务（设置 → SMS 配置）"
                                delay(5000)
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "🔐 [$provider] 使用协议模式（需要 SMS 验证）"

                            val password = generatePassword()
                            val regResult = engine.register(mailAccount.email)
                            if (!regResult.success) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 注册失败: ${regResult.error}"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "⏳ [$provider] 邮箱验证码已发送..."

                            // 等待邮箱验证码
                            val emailCode = tempMailClient.waitForVerificationCode(mailAccount, 120000L)
                            if (emailCode == null) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "⚠️ [$provider] 未收到邮箱验证码，跳过"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            if (!engine.continueWithEmailCode(mailAccount.email, emailCode)) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 邮箱验证码错误"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "📱 [$provider] 购买手机号中..."

                            // 购买手机号并发送验证码
                            if (!engine.buyAndSendPhoneCode()) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 购买手机号失败，请检查 SMS 余额"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "⏳ [$provider] 等待手机验证码..."

                            // 等待手机验证码
                            val phoneCode = smsService?.waitForCode(engine.phoneOrderId, 120000L)
                            if (phoneCode == null) {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "⚠️ [$provider] 未收到手机验证码，跳过"
                                delay(5000)
                                _accountSalts.value++
                                continue
                            }

                            val result = engine.completeWithPhoneCode(password, phoneCode)
                            if (result.success) {
                                val apiKey = result.apiKey ?: ""
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value + listOf(
                                    "🎉 [$provider] 注册成功!",
                                    "📧 ${mailAccount.email}",
                                    "🔑 API Key: ${apiKey.take(20)}..."
                                )
                                repository.insertKey(ApiKeyItem(
                                    keyLabel = "$provider 自动注册",
                                    keyValue = apiKey,
                                    provider = provider,
                                    status = "Active"
                                ))
                            } else {
                                _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                    "❌ [$provider] 注册失败: ${result.error}"
                            }
                        }

                        else -> {
                            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                                "⚠️ [$provider] 暂不支持"
                            delay(5000)
                        }
                    }

                    _accountSalts.value++
                    delay(8000)
                }
            }
        } else {
            _tempMailInboxLogs.value = _tempMailInboxLogs.value +
                "🛑 自动注册服务已休眠"
        }
    }

    // ==================== 真实网关 ====================
    private val _gatewayPort = MutableStateFlow(8000)
    val gatewayPort: StateFlow<Int> = _gatewayPort.asStateFlow()

    private val _gatewayActive = MutableStateFlow(false)
    val gatewayActive: StateFlow<Boolean> = _gatewayActive.asStateFlow()

    private val _gatewayRequestCount = MutableStateFlow(0)
    val gatewayRequestCount: StateFlow<Int> = _gatewayRequestCount.asStateFlow()

    private val _contextCapLimit = MutableStateFlow(40)
    val contextCapLimit: StateFlow<Int> = _contextCapLimit.asStateFlow()

    private val _tokenThreshold = MutableStateFlow(4096)
    val tokenThreshold: StateFlow<Int> = _tokenThreshold.asStateFlow()

    private val _openaiSimLogs = MutableStateFlow<List<String>>(listOf(
        "⚡ [Gateway] Ktor 引擎已就绪，待启动",
        "🌐 监听地址: 127.0.0.1:8000"
    ))
    val openaiSimLogs: StateFlow<List<String>> = _openaiSimLogs.asStateFlow()

    fun toggleGateway() {
        val current = _gatewayActive.value
        _gatewayActive.value = !current

        if (!current) {
            // 启动真实 Ktor 服务器
            gatewayServer.start()
            viewModelScope.launch {
                val count = keyPoolManager.getActiveKeyCount()
                _openaiSimLogs.value = _openaiSimLogs.value + listOf(
                    "⚡ [Gateway] 真实 Ktor 服务器已启动: 127.0.0.1:${_gatewayPort.value}",
                    "✅ 端点: POST /v1/chat/completions",
                    "✅ 健康检查: GET /health",
                    "✅ 模型列表: GET /v1/models",
                    "🔑 活跃密钥数: $count"
                )
            }
            // 启动前台服务保活
            GatewayForegroundService.start(context)
        } else {
            gatewayServer.stop()
            GatewayForegroundService.stop(context)
            _openaiSimLogs.value = _openaiSimLogs.value + listOf(
                "🛑 [Gateway] 真实 Ktor 服务器已停止"
            )
        }
    }

    fun changeGatewayPort(port: Int) {
        _gatewayPort.value = port
        if (_gatewayActive.value) {
            // 重启服务器
            gatewayServer.stop()
            // 需带新端口的实例
            _openaiSimLogs.value = _openaiSimLogs.value +
                "⚙️ [Gateway] 端口已更换为 $port，需重新启动生效"
            _gatewayActive.value = false
        }
    }

    fun setContextCapLimit(limit: Int) { _contextCapLimit.value = limit }
    fun setTokenThreshold(threshold: Int) { _tokenThreshold.value = threshold }

    // ==================== JSON 导出/备份 ====================
    fun exportApiKeysJson(): String {
        val keys = apiKeysList.value
        val array = org.json.JSONArray()
        keys.forEach { key ->
            val obj = org.json.JSONObject()
            obj.put("keyLabel", key.keyLabel)
            obj.put("keyValue", key.keyValue)
            obj.put("provider", key.provider)
            obj.put("status", key.status)
            obj.put("timestamp", key.timestamp)
            obj.put("poolType", key.poolType)
            array.put(obj)
        }
        return array.toString(2)
    }

    fun importApiKeysJson(jsonStr: String): Boolean {
        return try {
            val array = org.json.JSONArray(jsonStr)
            viewModelScope.launch {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val keyItem = ApiKeyItem(
                        keyLabel = obj.optString("keyLabel", "导入密钥"),
                        keyValue = obj.getString("keyValue"),
                        provider = obj.optString("provider", "Gemini"),
                        status = obj.optString("status", "Active"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        poolType = obj.optString("poolType", "active")
                    )
                    repository.insertKey(keyItem)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==================== 密钥列表 ====================
    val apiKeysList: StateFlow<List<ApiKeyItem>> = repository.allKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==================== 密钥验证 ====================
    private val _testResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val testResult: StateFlow<Pair<Boolean, String>?> = _testResult.asStateFlow()

    private val _isTestingKey = MutableStateFlow(false)
    val isTestingKey: StateFlow<Boolean> = _isTestingKey.asStateFlow()

    fun verifyApiKey(provider: String, key: String) {
        if (key.trim().isEmpty()) return
        _isTestingKey.value = true
        _testResult.value = null
        viewModelScope.launch {
            val result = repository.testApiKey(provider, key)
            _testResult.value = result
            _isTestingKey.value = false

            val currentList = apiKeysList.value
            val match = currentList.firstOrNull { it.keyValue == key.trim() }
            if (match != null) {
                repository.updateKey(match.copy(status = if (result.first) "Active" else "Expired"))
            } else {
                repository.insertKey(
                    ApiKeyItem(
                        keyLabel = "${provider} 手动导入并校验",
                        keyValue = key.trim(),
                        provider = provider,
                        status = if (result.first) "Active" else "Expired"
                    )
                )
            }
        }
    }

    fun clearTestResult() { _testResult.value = null }
    fun deleteSavedKey(id: Long) { viewModelScope.launch { repository.deleteKeyById(id) } }

    fun insertCustomKey(label: String, value: String, provider: String) {
        if (value.trim().isEmpty()) return
        viewModelScope.launch {
            repository.insertKey(
                ApiKeyItem(
                    keyLabel = label.ifEmpty { "${provider} 手动签发" },
                    keyValue = value.trim(),
                    provider = provider,
                    status = "Untested"
                )
            )
        }
    }

    fun clearAllSavedKeys() { viewModelScope.launch { repository.clearAllKeys() } }

    // ==================== UI 状态 ====================
    private val _currentSection = MutableStateFlow(0)
    val currentSection: StateFlow<Int> = _currentSection.asStateFlow()

    private val _language = MutableStateFlow("zh")
    val language: StateFlow<String> = _language.asStateFlow()

    fun selectSection(index: Int) { _currentSection.value = index }
    fun toggleLanguage() { _language.value = if (_language.value == "zh") "en" else "zh" }

    // ==================== Timer/Session 状态 ====================
    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer() {
        _timerState.value = TimerState.RUNNING
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _elapsedSeconds.value = _elapsedSeconds.value + 1
            }
        }
    }

    fun pauseTimer() {
        _timerState.value = TimerState.PAUSED
        timerJob?.cancel()
    }

    fun resetTimer() {
        _timerState.value = TimerState.IDLE
        timerJob?.cancel()
        _elapsedSeconds.value = 0L
    }

    // ==================== Auto Acquisition Mock ====================
    private val _acquisitionLogs = MutableStateFlow<List<String>>(emptyList())
    val acquisitionLogs: StateFlow<List<String>> = _acquisitionLogs.asStateFlow()

    private val _isAcquiring = MutableStateFlow(false)
    val isAcquiring: StateFlow<Boolean> = _isAcquiring.asStateFlow()

    private val _acquiredKeyResult = MutableStateFlow<ApiKeyItem?>(null)
    val acquiredKeyResult: StateFlow<ApiKeyItem?> = _acquiredKeyResult.asStateFlow()

    fun triggerAutoAcquisition(provider: String, label: String) {
        _isAcquiring.value = true
        _acquisitionLogs.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            val logs = listOf(
                "📡 连接 [${provider}] API 平台...",
                "🔐 建立 TLS 加密隧道...",
                "🗂️ 准备注册 $label...",
                "🔑 请使用自动注册功能完成真实注册"
            )
            for (log in logs) {
                delay(800)
                _acquisitionLogs.value = _acquisitionLogs.value + log
            }
            _isAcquiring.value = false
        }
    }

    // ==================== Termux / Shell 执行 ====================
    private val _termuxOutput = MutableStateFlow<List<String>>(listOf("🖥️ Termux 终端就绪"))
    val termuxOutput: StateFlow<List<String>> = _termuxOutput.asStateFlow()

    fun executeTermuxCommand(cmd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _termuxOutput.value = _termuxOutput.value + "\$ $cmd"
            try {
                val process = Runtime.getRuntime().exec(cmd)
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val errorReader = java.io.BufferedReader(java.io.InputStreamReader(process.errorStream))
                val output = reader.readText()
                val error = errorReader.readText()
                process.waitFor()
                if (output.isNotEmpty()) {
                    _termuxOutput.value = _termuxOutput.value + output.take(500).lines()
                }
                if (error.isNotEmpty()) {
                    _termuxOutput.value = _termuxOutput.value + error.take(500).lines()
                }
            } catch (e: Exception) {
                _termuxOutput.value = _termuxOutput.value + "错误: ${e.message}"
            }
        }
    }

    // ==================== 终端模拟 / 网关仿真 ====================
    val terminalOutputState = MutableStateFlow("")

    fun triggerGatewaySimulation(prompt: String) {
        viewModelScope.launch {
            _openaiSimLogs.value = _openaiSimLogs.value + "[SIM] $prompt"
        }
    }

    fun executeTerminalCommand(cmd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                val output = process.inputStream.bufferedReader().readText()
                terminalOutputState.value = output
            } catch (e: Exception) {
                terminalOutputState.value = "Error: ${e.message}"
            }
        }
    }

    // ==================== 密码生成 ====================
    private fun generatePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%"
        return (1..16).map { chars.random() }.joinToString("")
    }

    // ==================== Factory ====================
    class Factory(
        private val repository: AuraRepository,
        private val filesDir: File,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuraViewModel(repository, filesDir, context) as T
        }
    }

    override fun onCleared() {
        super.onCleared()
        gatewayServer.stop()
    }
}