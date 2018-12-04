
import javax.sound.midi.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class BeatBox {

    JPanel mainPanel;
    ArrayList<JCheckBox> checkBoxList;//Храним флажки в массиве ArrayList;
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;
    String [] instrumentNames={"Bass Drum","Closed Hi-Hat","Open Hi-Hat","Acoustic Snare","Crash Cymbal", "Hand Clap","High Tom","Hi Bongo",
            "Maracas","Whistle","Low Conga","Cowbell","Vibraslap","Low-mid Tom","High Agogo","Open Hi Conga"};//название инструментов для меток

    int [] instruments={35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};//Числа представляют собой фактические барабанные клавиши.Канал барабана что то вроде фортеиано, только каждая клавиша на нем отдельный барабан

    public static void main(String [] args){
        new BeatBox().buildGUI();
    }
    public void buildGUI(){
        theFrame=new JFrame("BeatBox Руслан Холов");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout=new BorderLayout();
        JPanel background=new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));//Пустая граница позволяет создать поля между краями панели и естом для размещения компонентов

        checkBoxList=new ArrayList<JCheckBox>();
        Box buttonBox=new Box(BoxLayout.Y_AXIS);

        JButton start=new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop=new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo=new JButton("Tempo up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo=new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        Box nameBox=new Box(BoxLayout.Y_AXIS);
        for(int i=0;i<16;i++){
            nameBox.add(new Label(instrumentNames[i]));
        }
        background.add(BorderLayout.EAST,buttonBox);
        background.add(BorderLayout.WEST,nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid=new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel=new JPanel(grid);
        background.add(BorderLayout.CENTER,mainPanel);

        for(int i=0;i<256;i++){
            JCheckBox c=new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);//Создаем флажки, добавляем в ArrayList и на панель
        }

        setUpMidi();

        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);


    }

    public void setUpMidi(){
        try{
            sequencer=MidiSystem.getSequencer();
            sequencer.open();
            sequence=new Sequence(Sequence.PPQ,4);
            track=sequence.createTrack();
            sequencer.setTempoInBPM(120);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void buildTrackandStart(){
        int [] trackList=null;//создаем массив из 16 элементов, чтобы хранить значение для каждого инструмента на все 16 тактов

        sequence.deleteTrack(track);//избавляемся от старой дорожки и создаем новую
        track=sequence.createTrack();

        for(int i=0;i<16;i++){
            trackList=new int[16];//Делаем это для каждого из 16 рядов

            int key=instruments[i];//Задаем клавишу, которая представляет инструмент.Массив содержит миди-числа для каждого инструмента

            for(int j=0;j<16;j++){
                //Делаем это для каждого текущего ряда
                JCheckBox jc=(JCheckBox)checkBoxList.get(j+(16*i));
                if(jc.isSelected()){
                    trackList[j]=key;
                }else{          //Установлен ли флажок на этом такте?Если да, то помещаем значение клавиши в текущую ячейку массива(ячейку которая представляет такт).Если нет,то инструмент не должен играть в этом такте,поэтому присвоим 0
                    trackList[j]=0;
                }
            }
            makeTracks(trackList);
            track.add(makeEvent(176,1,127,0,16));//для этого инструмента и дл всех 16 тактов создаем события и добавляем в дорожку
        }
        track.add(makeEvent(192,9,1,0,15));//
        try{
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);//Позволяет задать количество повторений цикла, или непрерывный цикл
            sequencer.start();
            sequencer.setTempoInBPM(120);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public class MyStartListener implements ActionListener{
        public void actionPerformed(ActionEvent a){
            buildTrackandStart();//Внутренний класс слушателя для кнопок
        }
    }
    public class MyStopListener implements ActionListener{
        public void actionPerformed(ActionEvent a){
            sequencer.stop();//Внутренний класс слушателя для кнопок
        }
    }
    public class MyUpTempoListener implements ActionListener{
        public void actionPerformed(ActionEvent a){
            float tempoFactor=sequencer.getTempoFactor(); //Коэффициент темпа определяет темп синтезатора.По умолчанию он равен 1.0.Поэтому щелчком
            sequencer.setTempoFactor((float)(tempoFactor*1.03));//мыши можно изменить его на +/- 3%
        }
    }
    public class MyDownTempoListener implements ActionListener{
        public void actionPerformed(ActionEvent a){
            float tempoFactor=sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor*.97));
        }
    }

    public void makeTracks(int[]list){//Метод создает события для одного инструмента за каждый проход цикла для всех 16 тактов.Можно получить int []
                                        //для Bass Drum и каждый элемент массива будет содержать либо клавишу для этого инструмента,либо ноль
                                        //Если это ноль,то инструмент не должен играть на текущем такте.Иначе нужно создать событие и добавить его в
                                        //в дорожку
        for(int i=0;i<16;i++){
            int key=list[i];
            if(key!=0){
                track.add(makeEvent(144,9,key,100,i));//Создаем события включения и выключения и добавляем их в дорожку.
                track.add(makeEvent(128,9,key,100,i+1));
            }
        }
    }
    public MidiEvent makeEvent(int comd,int chan,int one,int two,int tick){
        MidiEvent event=null;
        try{
            ShortMessage a=new ShortMessage();
            a.setMessage(comd,chan,one,two);
            event=new MidiEvent(a,tick);
        }catch(Exception e){
            e.printStackTrace();
        }
        return event;
    }
}
