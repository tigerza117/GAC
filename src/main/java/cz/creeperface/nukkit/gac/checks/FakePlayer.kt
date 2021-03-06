package cz.creeperface.nukkit.gac.checks

import cn.nukkit.Player
import cn.nukkit.entity.Entity
import cn.nukkit.entity.data.EntityMetadata
import cn.nukkit.item.Item
import cn.nukkit.level.Location
import cn.nukkit.math.NukkitRandom
import cn.nukkit.math.Vector3
import cn.nukkit.network.protocol.AddPlayerPacket
import cn.nukkit.network.protocol.MovePlayerPacket
import cn.nukkit.network.protocol.SetEntityDataPacket
import cn.nukkit.utils.MainLogger
import cz.creeperface.nukkit.gac.GTAnticheat
import cz.creeperface.nukkit.gac.utils.CheckType
import cz.creeperface.nukkit.gac.utils.shouldCheck
import java.util.*

class FakePlayer : Location() {

    lateinit var player: Player
    var metadata = EntityMetadata()

    private val random = NukkitRandom()

    private var spawnCount = 0

    fun init(p: Player) {
        player = p

        if (!GTAnticheat.conf.enabled(CheckType.AIMBOT)) {
            return
        }

        val pos = player.position.add(player.directionVector.normalize())

        this.setComponents(pos.x, pos.y, pos.z)

        this.metadata = EntityMetadata()
                .putLong(Entity.DATA_FLAGS, (1 shl Entity.DATA_FLAG_SILENT or (1 shl Entity.DATA_FLAG_INVISIBLE)).toLong()/* | (1 << Entity.DATA_FLAG_CAN_SHOW_NAMETAG) | (1 << Entity.DATA_FLAG_ALWAYS_SHOW_NAMETAG)*/)
                .putString(Entity.DATA_NAMETAG, "safasfasfasfasfa")
                //.putBoolean(Entity.DATA_FLAG_CAN_SHOW_NAMETAG, false)
                //.putBoolean(Entity.DATA_FLAG_NO_AI, true)
                .putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
                .putFloat(Entity.DATA_SCALE, 0f)
                .putFloat(Entity.DATA_BOUNDING_BOX_HEIGHT, 0f)
                .putFloat(Entity.DATA_BOUNDING_BOX_WIDTH, 0f)
        //.putByte(Entity.DATA_LEAD, 0);

        changeName()
        spawn()
    }

    fun update(pos: Location) {
        update(null, pos)
    }

    fun update(from: Location?, to: Location) {
        if (!shouldCheck(this.player, CheckType.AIMBOT)) {
            return
        }

        val pos: Vector3 = if (from != null && this.distance(from.x, from.z) > distance(to.x, to.z)) {
            to.add(getDirectionVector(to).normalize().multiply(-3.0))
        } else {
            to.add(getDirectionVector(to).normalize().multiply(-1.0))
        }

        this.setComponents(pos.x, pos.y, pos.z)
        changeName()

        spawnCount++

        if (spawnCount > 200) {
            spawnCount = 0

            spawn()
        } else {
            val pk = MovePlayerPacket()
            pk.eid = ID
            pk.x = x.toFloat()
            pk.y = (y + 1.62).toFloat()
            pk.z = z.toFloat()
            pk.yaw = yaw.toFloat()
            pk.headYaw = yaw.toFloat()
            pk.pitch = pitch.toFloat()
            pk.onGround = false

            player.dataPacket(pk)
        }
    }

    private fun spawn() {
        val pk = AddPlayerPacket()
        pk.uuid = UUID.randomUUID()
        pk.username = this.metadata.getString(Entity.DATA_NAMETAG)
        pk.entityRuntimeId = ID
        pk.entityUniqueId = ID
        pk.x = this.x.toFloat()
        pk.y = (this.y + 1.62).toFloat()
        pk.z = this.z.toFloat()
        pk.speedX = 0f
        pk.speedY = 0f
        pk.speedZ = 0f
        pk.yaw = 0f
        pk.pitch = 0f
        pk.metadata = metadata
        pk.item = Item.get(Item.AIR)

        player.dataPacket(pk)
    }

    private fun changeName(): String {
        val chars = CharArray(random.nextBoundedInt(12) + 1)

        for (i in chars.indices) {
            chars[i] = (random.nextBoundedInt('z'.toInt()) + 1 + '0'.toInt()).toChar()
        }

        val name = "001" + String(chars)

        metadata.putString(Entity.DATA_NAMETAG, name)

        val pk = SetEntityDataPacket()
        pk.eid = ID
        pk.metadata = metadata
        this.player.dataPacket(pk)

        return name
    }

    private fun getDirectionVector(p: Location): Vector3 {
        val pitch = p.getPitch() * Math.PI / 180.0
        val yaw = (p.getYaw() + 30 + random.nextRange(-10, 10).toDouble() + 90.0) * Math.PI / 180.0
        val x = Math.sin(pitch) * Math.cos(yaw)
        val z = Math.sin(pitch) * Math.sin(yaw)
        val y = Math.cos(pitch)
        return Vector3(x, y, z).normalize()
    }

    private fun distance(x: Double, z: Double): Double {
        return Math.sqrt(Math.pow(this.x - x, 2.0) + Math.pow(this.z - z, 2.0))
    }

    companion object {

        const val ID = -8372514832L
    }
}
