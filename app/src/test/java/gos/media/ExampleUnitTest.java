package gos.media;

import com.alibaba.fastjson.JSON;

import org.junit.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

import gos.media.data.Date;
import gos.media.data.Device;
import gos.media.data.EpgProgram;
import gos.media.data.IndexClass;
import gos.media.data.Program;
import gos.media.data.ProgramUrl;
import gos.media.data.ReserveEventSend;
import gos.media.data.Respond;
import gos.media.data.Time;
import gos.media.define.DataPackage;
import gos.media.define.NetProtocol;
import gos.media.define.*;

import static gos.media.define.CommandType.*;   //导入静态命令集

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    String[] testUrl = new String[]{
            "http://192.168.1.109:1026/out/enc_name.m3u8",
            "http://192.168.100.103/hls_media_1.m3u8" ,
    "http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8",
    "http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8" ,
    "http://playertest.longtailvideo.com/adaptive/oceans_aes/oceans_aes.m3u8" ,
    "http://playertest.longtailvideo.com/adaptive/captions/playlist.m3u8",
    };

    InetAddress local =null;
    ArrayList<EpgProgram> epgPrograms = createEpgPrograms(5);
    @Test
    public void addition_isCorrect() throws Exception {
        startServer();
        //test();
    }


    public void startServer()throws IOException{
        /*
         * 接收客户端发送的数据
         */
        // 1.创建服务器端DatagramSocket，指定端口
        local = InetAddress.getLocalHost();
        DatagramSocket socket = new DatagramSocket(NetProtocol.sendPort,local);
        System.out.println("服务器配置");
        System.out.println("ip:"+socket.getLocalAddress());
        System.out.println("port:"+socket.getLocalPort());

        // 2.创建数据报，用于接收客户端发送的数据
        byte[] data = new byte[1024];// 创建字节数组，指定接收的数据包的大小
        DatagramPacket packet = new DatagramPacket(data, data.length);
        // 3.接收客户端发送的数据
        System.out.println("****服务器端已经启动，等待客户端发送数据");
        boolean flag = true;
        while(flag)
        {
            socket.receive(packet);// 此方法在接收到数据报之前会一直阻塞
            byte[] recvData = Arrays.copyOf(data,packet.getLength());
            try{
                byte[] sendData =  parseData(recvData);
                if(null != sendData)
                {
                    /*向客户端响应数据*/
                    InetAddress address = packet.getAddress();

                    DatagramPacket packet2 = new DatagramPacket(sendData, sendData.length, address, NetProtocol.receivePort);
                    System.out.println("服务器配置");
                    System.out.println("ip:"+address.getHostAddress());
                    System.out.println("port:"+packet2.getPort());
                    socket.send(packet2);
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        // 4.关闭资源
        socket.close();
    }


    private byte[] parseData(byte[] reaciveData)throws Exception{
        DataPackage dataPackage = new DataPackage(reaciveData);

        String sendData = null;
        Respond respond = null;
        //System.out.println("命令："+dataPackage.getCommand());
        switch (dataPackage.getCommand()){

             /* 系统命令 */
            case COM_SYSTEM_RESPOND :
                respond = DataParse.getRespond(dataPackage.getData());
               // System.out.println("回应"+respond.getCommand());
                break;     //回应
            case COM_SYSTEM_HEARTBEAT_PACKET :
                System.out.println("心跳包");
                try{
                    Thread.sleep(1000);
                    dataPackage.command = COM_SYSTEM_HEARTBEAT_PACKET;
                }catch (Exception e){
                    e.printStackTrace();}
                break;    //

            /* 连接模块命令*/
            case COM_CONNECT_GET_DEVICE:
                System.out.println("查找设备");
                dataPackage.command = COM_CONNECT_SET_DEVICE;

                Device device = new Device(local.getHostAddress(),"ff:ee:34:63:23","2017");
                sendData = JSON.toJSONString(device);
                break;      //

            case COM_CONNECT_ATTACH  :
                System.out.println("连接设备");
                respond = new Respond(dataPackage.getCommand(),true);
                dataPackage.setCommand(COM_SYSTEM_RESPOND);
                sendData = JSON.toJSONString(respond);
                break;     //
            case COM_CONNECT_DETACH  :
                System.out.println("分离");
                respond = new Respond(dataPackage.getCommand(),true);
                dataPackage.setCommand(COM_SYSTEM_RESPOND);
                sendData = JSON.toJSONString(respond);
                break;    //

             /* 直播模块命令 */
            case COM_LIVE_GET_PROGRAM_LIST :
                System.out.println("获取节目列表");
                dataPackage.command = COM_LIVE_SET_PROGRAM_LIST;

                sendData = JSON.toJSONString(getProgramList());
                break; //
            case COM_LIVE_GET_PROGRAM_URL :
                System.out.println("获取节目url");
                //String testUrl ="
                dataPackage.command = COM_LIVE_SET_PROGRAM_URL;
                IndexClass indexClass = JSON.parseObject(dataPackage.getData(),IndexClass.class);
                int index = indexClass.getIndex();
                if(index<testUrl.length){
                    ProgramUrl programUrl = new ProgramUrl(index,testUrl[index]);
                    sendData = JSON.toJSONString(programUrl);
                }

                break; //

            case COM_LIVE_STOP_PROGRAM :
                System.out.println("停止节目");
                respond = new Respond(dataPackage.getCommand(),true);
                dataPackage.setCommand(COM_SYSTEM_RESPOND);
                sendData = JSON.toJSONString(respond);
                break;   //

            case COM_EPG_GET_SELECT_PROGRAM:
                System.out.println("获取节目epg");
                IndexClass indexClass1 = DataParse.getIndexClass(dataPackage.getData());
                dataPackage.setCommand(COM_EPG_SET_SELECT_PROGRAM);
                sendData = JSON.toJSONString(epgPrograms.get(indexClass1.getIndex()) );
                break;
            case COM_EPG_SET_RESERVE:
                System.out.println("设置预定事件");
                ReserveEventSend reserveEventSend = DataParse.getReserveEventSend(dataPackage.getData());


                EpgProgram epgProgram = epgPrograms.get(reserveEventSend.getIndex());

                System.out.println("事件ID:"+reserveEventSend.getEventId());
                System.out.println("事件类型:"+reserveEventSend.getEventType());

                Time setTime = getTimeByEventId(epgProgram.getDateArray(),reserveEventSend.getEventId());
                if(null != setTime){
                    setTime.setEventType(reserveEventSend.getEventType());
                    respond = new Respond(dataPackage.getCommand(),true);
                    dataPackage.setCommand(COM_SYSTEM_RESPOND);
                    sendData = JSON.toJSONString(respond);
                    System.out.println("发送回应");
                }


            default:
                break;
        }
        if(null == sendData)
        {
            return null;
        }
        System.out.println(sendData);
        dataPackage.setData(sendData);
        return dataPackage.toByte();
    }


    ArrayList<EpgProgram> createEpgPrograms(int num){
        ArrayList<EpgProgram> epgPrograms = new ArrayList<>();
        for(int i=0;i<num;i++){
            EpgProgram epgProgram = createEpgProgram(i);
            epgPrograms.add(epgProgram);
        }

        return epgPrograms;
    }

    EpgProgram createEpgProgram(int index){
        EpgProgram epgProgram = new EpgProgram();
        epgProgram.setName(String.format("cctv-%d",index));
        epgProgram.setIndex(index);
        epgProgram.setLcn(1234);
        epgProgram.setServiceId(10);

        ArrayList<Date> dateArrayList = epgProgram.getDateArray();
        int position = 0;
        for(int i=0;i<5;i++){
            Date date = new Date();
            date.setDate(String.format("2017-08-%d",i+15));
            ArrayList<Time> timeArrayList = date.getTimeArray();
            for(int j=0;i<5;i++){
                Time time = new Time();
                time.setEventType("1");
                time.setStartTime(String.format("%d:00",i));
                time.setEndTime(String.format("%d:00",i+1));
                time.setShortDes("short des");
                time.setEvent(String.format("bbc-%d",i+1));
                time.setEventID(String.valueOf(position++));

                timeArrayList.add(time);
            }

            date.setTimeArray(timeArrayList);
            dateArrayList.add(date);
        }

        return epgProgram;
    }

    ArrayList<Program> getProgramList(){

        ArrayList<Program> programList = new ArrayList<>();
        for(int i = 0;i<epgPrograms.size();i++)
        {
            EpgProgram epgProgram = epgPrograms.get(i);
            Program program = new Program(epgProgram.getName(),epgProgram.getIndex());
            programList.add(program);
        }

        return programList;
    }

    Time getTimeByEventId(ArrayList<Date> dates, String id){
        for (int j=0;j<dates.size();j++){
            ArrayList<Time> times = dates.get(j).getTimeArray();
            for(int i=0;i<times.size();i++){
                Time time = times.get(i);

                if(id.equals(time.getEventID())){
                    return time;
                }
            }
        }
        return null;
    }

}