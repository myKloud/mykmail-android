package com.fsck.k9.backends

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.account.AccountCreator
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.backend.jmap.JmapDiscoveryResult.JmapAccount
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.LocalStoreProvider

class JmapAccountCreator(
    private val preferences: Preferences,
    private val backendManager: BackendManager,
    private val accountCreator: AccountCreator,
    private val localStoreProvider: LocalStoreProvider
) {
    fun createAccount(emailAddress: String, password: String, jmapAccount: JmapAccount) {
        val serverSettings = createServerSettings(emailAddress, password, jmapAccount)

        val account = preferences.newAccount().apply {
            email = emailAddress
            description = jmapAccount.name
            incomingServerSettings = serverSettings
            outgoingServerSettings = serverSettings

            chipColor = accountCreator.pickColor()
            deletePolicy = Account.DeletePolicy.ON_DELETE
        }

        createOutboxFolder(account)
        preferences.saveAccount(account)

        fetchFolderList(account)
    }

    private fun createServerSettings(emailAddress: String, password: String, jmapAccount: JmapAccount): ServerSettings {
        return ServerSettings(
            "jmap",
            null,
            433,
            ConnectionSecurity.SSL_TLS_REQUIRED,
            AuthType.PLAIN,
            emailAddress,
            password,
            null,
            mapOf("accountId" to jmapAccount.accountId)
        )
    }

    private fun createOutboxFolder(account: Account) {
        val localStore = localStoreProvider.getInstance(account)
        account.outboxFolderId = localStore.createLocalFolder(Account.OUTBOX_NAME, FolderType.OUTBOX)
    }

    private fun fetchFolderList(account: Account) {
        val backend = backendManager.getBackend(account)
        backend.refreshFolderList()
    }
}
