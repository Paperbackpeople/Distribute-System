import Client.GamerNode;
import java.util.Scanner;

class Game {
public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java Game [IP-address] [port-number] [player-id]");
            return;
        }

        String trackerIp = args[0];
        int trackerPort = Integer.parseInt(args[1]);
        String playerId = args[2];

        try {
            Scanner scanner = new Scanner(System.in);
            // 创建并启动游戏
            GamerNode gamerNode = new GamerNode(trackerIp, trackerPort, playerId, scanner);
            Thread.sleep(2000);
            gamerNode.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
