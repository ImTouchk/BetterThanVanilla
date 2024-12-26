package dev.mugur.btv.misc

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import dev.mugur.btv.utils.ChatCommand
import dev.mugur.btv.utils.ChatHelper
import org.bukkit.entity.Player

class DepartmentCommands {
    companion object {
        fun department(): ChatCommand  {
            return ChatCommand("department")
                .subcommand(join())
        }

        private fun join() : ChatCommand {
            return ChatCommand("join")
                .requirePlayerSender()
                .argument("name", StringArgumentType.word())
                .executes { ctx ->
                    val player = ctx.source.sender as Player
                    val name = StringArgumentType.getString(ctx, "name")
                    val dept = Department.getFromName(name)
                        ?: return@executes ChatHelper.sendMessage(ctx, "dept.error.invalid_name", name)

                    player.setDepartment(dept)
                    ChatHelper.sendMessage(ctx, "dept.success.changed", dept.color.asHexString(), name.uppercase())
                }
        }
    }
}