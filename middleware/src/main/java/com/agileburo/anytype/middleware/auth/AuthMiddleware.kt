package com.agileburo.anytype.middleware.auth

import anytype.Events
import anytype.model.Models
import com.agileburo.anytype.data.auth.model.AccountEntity
import com.agileburo.anytype.data.auth.model.WalletEntity
import com.agileburo.anytype.data.auth.repo.AuthRemote
import com.agileburo.anytype.middleware.EventProxy
import com.agileburo.anytype.middleware.interactor.Middleware
import com.agileburo.anytype.middleware.toAccountEntity
import com.agileburo.anytype.middleware.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AuthMiddleware(
    private val middleware: Middleware,
    private val events: EventProxy
) : AuthRemote {

    override suspend fun startAccount(
        id: String, path: String
    ) = middleware.selectAccount(id, path).let { response ->
        AccountEntity(
            id = response.id,
            name = response.name,
            avatar = if (response.avatar.avatarCase == Models.Account.Avatar.AvatarCase.IMAGE)
                response.avatar.image.toEntity() else null,
            color = if (response.avatar.avatarCase == Models.Account.Avatar.AvatarCase.COLOR)
                response.avatar.color else null
        )
    }

    override suspend fun createAccount(
        name: String,
        avatarPath: String?
    ) = withContext(Dispatchers.IO) {
        middleware.createAccount(name, avatarPath).let { response ->
            AccountEntity(
                id = response.id,
                name = response.name,
                avatar = if (response.avatar.avatarCase == Models.Account.Avatar.AvatarCase.IMAGE)
                    response.avatar.image.toEntity() else null,
                color = if (response.avatar.avatarCase == Models.Account.Avatar.AvatarCase.COLOR)
                    response.avatar.color else null
            )
        }
    }

    override suspend fun recoverAccount() = withContext(Dispatchers.IO) {
        middleware.recoverAccount()
    }

    override fun observeAccounts() = events
        .flow()
        .filter { event -> event.messageCase == Events.Event.MessageCase.ACCOUNTSHOW }
        .map { event -> event.accountShow.toAccountEntity() }

    override suspend fun createWallet(
        path: String
    ) = WalletEntity(mnemonic = middleware.createWallet(path).mnemonic)

    override suspend fun recoverWallet(path: String, mnemonic: String) {
        middleware.recoverWallet(path, mnemonic)
    }
}