import javax.imageio.ImageIO
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService

class GameListener {

    static final Integer INTERVAL_TIME = 2 * 1000

    String path
    String savePath
    WatchService watchService

    GameListener(String path,String savePath){
        this.path = path
        this.savePath = savePath
        init()
    }

    def init(){
        watchService = FileSystems.getDefault().newWatchService()
        Paths.get(path).register this.watchService,StandardWatchEventKinds.ENTRY_MODIFY
    }

    def start(){
        def screenSnapshot = {String path ->
            int width = Toolkit.getDefaultToolkit().getScreenSize().getWidth()
            int height = Toolkit.getDefaultToolkit().getScreenSize().getHeight()
            Robot robot = new Robot()
            BufferedImage image = robot.createScreenCapture(new Rectangle(width,height))
            ImageIO.write (image, "png" , new File(path,'截圖.png'))
        }

        def copyFolder = {String path ->
            new AntBuilder().copy(todir: path){
                fileset(dir: this.path)
            }
        }

        new Thread({
            while(true){
                def key = watchService.take()
                for (WatchEvent<Path> event: key.pollEvents()) {
                    def save = savePath.concat(File.separator).concat(new Date().format('yyyy-MM-dd HH-mm-ss') as String).concat File.separator
                    if(new File(save).mkdir()) {
                        try{screenSnapshot(save)}catch(e){} //只是爲了截圖失敗後還能正常存檔。
                        copyFolder(save)
                    }
                }
                if(!key.reset()) break
                Thread.sleep(INTERVAL_TIME)
            }
        }).start()
    }

    static void main(String[] args) {
        new GameListener('D:\\Games\\Dead Cells\\SteamEmu\\Saves','D:\\Games\\Dead Cells\\backup').start()
        println System.in.read()
    }

}