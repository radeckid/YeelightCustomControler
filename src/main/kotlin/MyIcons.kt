import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun getAppIcon(): BufferedImage {
    val file = File("src${File.separator}main${File.separator}assets${File.separator}ikonaLogo.png")
    return ImageIO.read(file)
}