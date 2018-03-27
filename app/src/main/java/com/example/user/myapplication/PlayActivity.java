package com.example.user.myapplication;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.lang.Thread.sleep;

public class PlayActivity extends AppCompatActivity {

    private NetworkTask client;

    private EditText inputWord;

    private Handler mHandler;

    private long lastTimeBackPressed;
    private String word = ""; // 입력 단어
    private String order; // 현재 순서 (시작은 무조건 첫 순서)
    private String fixOrder; // 내 순서 (intent로 받음)
    private String roomNumber; // 방 번호 (intent로 받음)
    private String hintWord; // 힌트를 통해 얻은 단어
    private String clickItem; // 클릭한 아이템
    private String roomLeader; // 방장 번호
    private int jumpCnt = 1, backCnt = 1, hintCnt = 1; // JUMP, BACK, HINT는 라운드 당 한 번만 사용 가능, 매 라운드 시작 시 1로 재할당
    private int myCount = 10, tmpCnt = 1, manCnt, roundCnt, nowRound;
    private boolean roomOut, send, gameStart, roundStart, hintClick, jumpClick, backClick;
    private ImageView turn1, turn2, turn3, turn4, user1, user2, user3, user4, right, wrong;
    private ImageView complete1, complete2, complete3, complete4, complete5, complete6, lose, win, gameover;
    TextView problem1, problem2, problem3, problem4, problem5, problem6, rdNum, preWord;

    private Timer timer = new Timer(11 * 1000, 1000);
    private Button out, hint, jump, back, start, enter;
    char[] mDataset;

    TextView mText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        Intent intent = getIntent();
        roomNumber = intent.getExtras().getString("num"); // 방 번호
        fixOrder = intent.getExtras().getString("leader"); // 고유 순서

        TextView rmNum = (TextView) findViewById(R.id.rmNum);
        TextView myNum = (TextView) findViewById(R.id.myNum);
        rmNum.setText(roomNumber);
        int myNumber = Integer.parseInt(fixOrder) + 1;
        myNum.setText(Integer.toString(myNumber));

        out = (Button) findViewById(R.id.out);
        hint = (Button) findViewById(R.id.hint);
        jump = (Button) findViewById(R.id.jump);
        back = (Button) findViewById(R.id.back);
        start = (Button) findViewById(R.id.start);
        enter = (Button) findViewById(R.id.enter);
        problem1 = (TextView) findViewById(R.id.problem1);
        problem2 = (TextView) findViewById(R.id.problem2);
        problem3 = (TextView) findViewById(R.id.problem3);
        problem4 = (TextView) findViewById(R.id.problem4);
        problem5 = (TextView) findViewById(R.id.problem5);
        problem6 = (TextView) findViewById(R.id.problem6);
        preWord = (TextView) findViewById(R.id.preWord);
        rdNum = (TextView) findViewById(R.id.rdNum);
        right = (ImageView) findViewById(R.id.right);
        wrong = (ImageView) findViewById(R.id.wrong);
        win = (ImageView) findViewById(R.id.win);
        lose = (ImageView) findViewById(R.id.lose);
        gameover = (ImageView) findViewById(R.id.gameover);
        complete1 = (ImageView) findViewById(R.id.complete1);
        complete2 = (ImageView) findViewById(R.id.complete2);
        complete3 = (ImageView) findViewById(R.id.complete3);
        complete4 = (ImageView) findViewById(R.id.complete4);
        complete5 = (ImageView) findViewById(R.id.complete5);
        complete6 = (ImageView) findViewById(R.id.complete6);

        Button.OnClickListener myClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.hint:
                        hintClick = true;
                        break;
                    case R.id.jump:
                        jumpClick = true;
                        break;
                    case R.id.back:
                        backClick = true;
                        break;
                    case R.id.out:
                        roomOut = true;
                        break;
                    case R.id.start:
                        if (fixOrder.equals(roomLeader) && manCnt == 2)
                            gameStart = true;
                        else
                            gameStart = false;
                        break;
                    case R.id.enter:
                        send = true;
                        break;
                }
            }
        };

        hint.setOnClickListener(myClickListener);
        jump.setOnClickListener(myClickListener);
        back.setOnClickListener(myClickListener);
        out.setOnClickListener(myClickListener);
        start.setOnClickListener(myClickListener);
        enter.setOnClickListener(myClickListener);

        roomOut = false;
        send = false;
        gameStart = false;
        roundStart = false;
        hintClick = false;
        jumpClick = false;
        backClick = false;

        turn1 = (ImageView) findViewById(R.id.turn1);
        turn2 = (ImageView) findViewById(R.id.turn2);
        turn3 = (ImageView) findViewById(R.id.turn3);
        turn4 = (ImageView) findViewById(R.id.turn4);
        user1 = (ImageView) findViewById(R.id.user1);
        user2 = (ImageView) findViewById(R.id.user2);
        user3 = (ImageView) findViewById(R.id.user3);
        user4 = (ImageView) findViewById(R.id.user4);
        wrong = (ImageView) findViewById(R.id.wrong);
        right = (ImageView) findViewById(R.id.right);

        inputWord = (EditText) findViewById(R.id.inputWord); // 입력한 단어
        mHandler = new Handler();

        client = new NetworkTask();
        client.execute();

        mText=(TextView) findViewById(R.id.timeText);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            /* 액티비티 종료 시 AsyncTask 종료 */
            if (client.getStatus() == AsyncTask.Status.RUNNING) {
                client.cancel(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* 뒤로가기 막기 */
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - lastTimeBackPressed < 1) {
            finish();
            return;
        }
        lastTimeBackPressed = System.currentTimeMillis();
    }

    public class NetworkTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                SocketHandler pSocket = (SocketHandler) getApplicationContext();
                InOutUpdate myUpdate = new InOutUpdate();
                byte[] a = new byte[1];
                SocketHandler.bis.read(a, 0, 1);
                String existCnt = new String(a); // 현재 방에 있는 사람 수
                if (!existCnt.equals("0")) {
                    byte[] b = new byte[Integer.parseInt(existCnt)];
                    SocketHandler.bis.read(b, 0, Integer.parseInt(existCnt));
                    String existOrders = new String(b); // 현재 방에 있는 사람들의 fixOrder
                    char[] orderSets = existOrders.toCharArray();
                    for (int i = 0; i < orderSets.length; i++) {
                        switch (orderSets[i]) {
                            case '0':
                                mHandler.post(setInUser1);
                                break;
                            case '1':
                                mHandler.post(setInUser2);
                                break;
                            case '2':
                                mHandler.post(setInUser3);
                                break;
                            case '3':
                                mHandler.post(setInUser4);
                                break;
                            default:
                                break;
                        }
                    }
                }
                while (!gameStart && isCancelled() == false) {
                    /* 게임 시작 대기 */
                    try {
                        myUpdate.start();
                    } catch (IllegalThreadStateException e) {
                        if (myUpdate.isUsed) {
                            myUpdate = new InOutUpdate();
                        }
                    }

                    if (roomOut) {
                        try {
                            System.out.println("roomOut : true");
                            /* 방 퇴장에 대한 데이터 송신 */
                            String proto = "4" + roomNumber; // 타입, 방 번호
                            ByteBuffer buffer = ByteBuffer.allocate(2);
                            buffer.order(ByteOrder.LITTLE_ENDIAN);
                            buffer.put(proto.getBytes("UTF-8"));
                            byte[] result = new byte[2];
                            result = buffer.array();
                            buffer.flip();
                            pSocket.bos.write(result);
                            pSocket.bos.flush();

                            /* 방 퇴장에 대한 데이터 수신 */
                            roomOut = false;
                            myUpdate.join();
                            client.cancel(true);
                            finish();
                        } catch (InterruptedException e) {

                        }
                    }

                    while (manCnt == 2 && !gameStart) {
                        if (!gameStart) {
                            try {
                                myUpdate.start();
                            } catch (IllegalThreadStateException e) {
                                if (myUpdate.isUsed) {
                                    myUpdate = new InOutUpdate();
                                }
                            }
                        }
                        if (roomOut) {
                            try {
                                System.out.println("roomOut : true");
                                /* 방 퇴장에 대한 데이터 송신 */
                                String proto = "4" + roomNumber; // 타입, 방 번호
                                ByteBuffer buffer = ByteBuffer.allocate(2);
                                buffer.order(ByteOrder.LITTLE_ENDIAN);
                                buffer.put(proto.getBytes("UTF-8"));
                                byte[] result = new byte[2];
                                result = buffer.array();
                                buffer.flip();
                                pSocket.bos.write(result);
                                pSocket.bos.flush();

                                /* 방 퇴장에 대한 데이터 수신 */
                                roomOut = false;
                                myUpdate.join();
                                client.cancel(true);
                                finish();
                            } catch (InterruptedException e) {

                            }
                        }
                        if (gameStart) {
                            /* 게임 시작 시(방장만) 서버에게 데이터 송신 */
                            order = roomLeader;
                            if (fixOrder.equals(roomLeader)) {
                                String proto = "7" + roomNumber; // 게임 시작
                                ByteBuffer buffer = ByteBuffer.allocate(2);
                                buffer.order(ByteOrder.LITTLE_ENDIAN);
                                buffer.put(proto.getBytes("UTF-8"));
                                byte[] result = new byte[2];
                                result = buffer.array();
                                buffer.flip();
                                pSocket.bos.write(result);
                                pSocket.bos.flush();
                            }
                            try {
                                myUpdate.join();
                            } catch (InterruptedException e) {

                            }
                            mHandler.post(outLock); // 나가기 버튼 잠금
                        }
                        /* 게임 중 */
                        while (gameStart && isCancelled() == false) {
                            /* 서버로부터 제시어 받음 */
                            byte[] c = new byte[18];
                            pSocket.bis.read(c, 0, 18);
                            String roundWord = new String(c, "UTF-8"); // 제시어 6글자
                            /* UI 작업 */
                            System.out.println(roundWord);
                            mDataset = roundWord.toCharArray();
                            mHandler.post(setProblem);
                            roundCnt = roundWord.length(); // 제시어의 음절 수(6)
                            nowRound = 1;
                            System.out.println(nowRound);
                            mHandler.post(nextRound);
                            while (roundCnt > 0 && isCancelled() == false) {
                                roundStart = true;
                                timer.start();
                                mHandler.post(nullPreWord);
                                while (roundStart && isCancelled() == false) {
                                    /* 현재 순서와 내 순서가 같을 경우 단어 전송 가능(내 차례) */

                                    if(order.equals("0"))
                                        mHandler.post(setTurn1);
                                    else if(order.equals("1"))
                                        mHandler.post(setTurn2);
                                    else if(order.equals("2"))
                                        mHandler.post(setTurn3);
                                    else if(order.equals("3"))
                                        mHandler.post(setTurn4);

                                    if (fixOrder.equals(order)) {
                                        if (myCount == 0) {
                                            /* 서버에게 패배했다는 데이터 전송 */
                                            String proto = "9" + roomNumber + order; // 해당 사용자 패배
                                            ByteBuffer buffer = ByteBuffer.allocate(3);
                                            buffer.order(ByteOrder.LITTLE_ENDIAN);
                                            buffer.put(proto.getBytes("UTF-8"));
                                            byte[] result = new byte[3];
                                            result = buffer.array();
                                            buffer.flip();
                                            SocketHandler.bos.write(result);
                                            SocketHandler.bos.flush();

                                            byte[] b1 = new byte[1];
                                            pSocket.bis.read(b1, 0, 1);
                                            String res = new String(b1);
                                            System.out.println(res);
                                            byte[] b2 = new byte[1];
                                            pSocket.bis.read(b2, 0, 1);
                                            order = new String(b2);
                                            System.out.println(order);
                                            byte[] b3 = new byte[9];
                                            pSocket.bis.read(b3, 0, 9);
                                            word = new String(b3, "UTF-8");
                                            mHandler.post(clearWord);
                                            mHandler.post(loseGame);
                                            myCount = 10;
                                            nowRound++;
                                            roundCnt--;
                                            roundStart = false;
                                            if (nowRound < 7) {
                                                mHandler.post(nextRound);
                                            }
                                        }
                                        mHandler.post(getWord);
                                        if (send) {
                                            if (word != null && word.length() == 3) {
                                                /* 단어 입력에 대한 데이터 송신 */
                                                String proto = "0" + roomNumber + fixOrder + word; // 타입, 단어
                                                ByteBuffer buffer = ByteBuffer.allocate(12);
                                                buffer.order(ByteOrder.LITTLE_ENDIAN);
                                                buffer.put(proto.getBytes("UTF-8"));
                                                byte[] result = new byte[12];
                                                result = buffer.array();
                                                buffer.flip();
                                                pSocket.bos.write(result);
                                                pSocket.bos.flush();

                                                /* 단어 입력(PASS, FAIL)에 대한 데이터 수신 */
                                                byte[] d1 = new byte[1];
                                                pSocket.bis.read(d1, 0, 1);
                                                String res = new String(d1); // 통과 여부
                                                byte[] d2 = new byte[1];
                                                pSocket.bis.read(d2, 0, 1);
                                                order = new String(d2); // 변경 순서
                                                byte[] d3 = new byte[9];
                                                pSocket.bis.read(d3, 0, 9);
                                                word = new String(d3, "UTF-8");
                                                if (res.equals("1")) { // 통과 시
                                                    mHandler.post(rightAnswer);
                                                    timer.cancel();
                                                    myCount = 10;
                                                    timer.start();
                                                }
                                                else {
                                                    mHandler.post(wrongAnswer);
                                                }
                                            } else {
                                                mHandler.post(wordErrorToast); // 단어 입력 오류
                                            }
                                            mHandler.post(setPreWord);
                                            send = false;
                                            word = ""; // 단어 초기화
                                            mHandler.post(clearWord);
                                        }
                                        /* 아이템은 라운드 당 1번 사용 가능 */
                                        if (hintClick) {
                                            clickItem = "HINT";
                                            if (hintCnt == 1) {
                                                mHandler.post(clearWord);
                                                /* 힌트 사용에 대한 데이터 송신 */
                                                String proto = "1" + roomNumber; // 힌트 사용
                                                ByteBuffer buffer = ByteBuffer.allocate(2);
                                                buffer.order(ByteOrder.LITTLE_ENDIAN);
                                                buffer.put(proto.getBytes("UTF-8"));
                                                byte[] result = new byte[2];
                                                result = buffer.array();
                                                buffer.flip();
                                                pSocket.bos.write(result);
                                                pSocket.bos.flush();

                                                /* 힌트 사용에 대한 데이터 수신 */
                                                byte[] d = new byte[9];
                                                pSocket.bis.read(d, 0, 9);
                                                hintWord = new String(d, "UTF-8");
                                                /* 힌트로 얻은 단어 EditText에 출력 */
                                                mHandler.post(setHintWord);
                                                hintCnt--;
                                            } else {
                                                mHandler.post(haveNotItemToast); // 아이템 사용 불가
                                            }
                                            hintClick = false;
                                            clickItem = "";
                                            word = ""; // 단어 초기화
                                        }
                                        if (jumpClick) {
                                            clickItem = "JUMP";
                                            if (jumpCnt == 1) {
                                            /* 점프 사용에 대한 데이터 송신 */
                                                String proto = "5" + roomNumber + order;// 점프 사용
                                                ByteBuffer buffer = ByteBuffer.allocate(3);
                                                buffer.order(ByteOrder.LITTLE_ENDIAN);
                                                buffer.put(proto.getBytes("UTF-8"));
                                                byte[] result = new byte[3];
                                                result = buffer.array();
                                                buffer.flip();
                                                pSocket.bos.write(result);
                                                pSocket.bos.flush();

                                                /* 점프 사용에 대한 데이터 수신 */
                                                byte[] b1 = new byte[1];
                                                pSocket.bis.read(b1, 0, 1);
                                                String res = new String(b1);
                                                byte[] b2 = new byte[1];
                                                pSocket.bis.read(b2, 0, 1);
                                                order = new String(b2); // 변경 순서
                                                byte[] b3 = new byte[9];
                                                pSocket.bis.read(b3, 0, 9);
                                                word = new String(b3, "UTF-8");
                                                timer.cancel();
                                                myCount = 10;
                                                timer.start();
                                                jumpCnt--;
                                            } else {
                                                mHandler.post(haveNotItemToast); // 아이템 사용 불가
                                            }
                                            jumpClick = false;
                                            clickItem = "";
                                            word = ""; // 단어 초기화
                                        }
                                        if (backClick) {
                                            clickItem = "BACK";
                                            if (backCnt == 1) {
                                                /* 백 사용에 대한 데이터 송신 */
                                                String proto = "6" + roomNumber + order; // 백 사용
                                                ByteBuffer buffer = ByteBuffer.allocate(3);
                                                buffer.order(ByteOrder.LITTLE_ENDIAN);
                                                buffer.put(proto.getBytes("UTF-8"));
                                                byte[] result = new byte[3];
                                                result = buffer.array();
                                                buffer.flip();
                                                pSocket.bos.write(result);
                                                pSocket.bos.flush();

                                                /* 백 사용에 대한 데이터 수신 */
                                                byte[] b1 = new byte[1];
                                                pSocket.bis.read(b1, 0, 1);
                                                String res = new String(b1);
                                                byte[] b2 = new byte[1];
                                                pSocket.bis.read(b2, 0, 1);
                                                order = new String(b2); // 변경 순서
                                                byte[] b3 = new byte[9];
                                                pSocket.bis.read(b3, 0, 9);
                                                word = new String(b3, "UTF-8");
                                                timer.cancel();
                                                myCount = 10;
                                                timer.start();
                                                backCnt--;
                                            } else {
                                                mHandler.post(haveNotItemToast); // 아이템 사용 불가
                                            }
                                            backClick = false;
                                            clickItem = "";
                                            word = ""; // 단어 초기화
                                        }
                                    }
                                    else {
                                        byte[] b1 = new byte[1];
                                        pSocket.bis.read(b1, 0, 1);
                                        String result = new String(b1);
                                        System.out.println(result);
                                        byte[] b2 = new byte[1];
                                        pSocket.bis.read(b2, 0, 1);
                                        order = new String(b2);
                                        System.out.println(order);
                                        byte[] b3 = new byte[9];
                                        pSocket.bis.read(b3, 0, 9);
                                        word = new String(b3, "UTF-8");
                                        mHandler.post(setWord);
                                        mHandler.post(setPreWord);
                                        if (result.equals("0")) { // 단어 실패 시
                                            mHandler.post(wrongAnswer);
                                            mHandler.post(clearWord);
                                        }
                                        else if (result.equals("1")) { // 단어 성공 시
                                            mHandler.post(rightAnswer);
                                            mHandler.post(clearWord);
                                            timer.cancel();
                                            myCount = 10;
                                            timer.start();
                                        }
                                        else if (result.equals("2")) { // 라운드 종료
                                            nowRound++;
                                            if (nowRound < 7) {
                                                mHandler.post(nextRound);
                                            }
                                            mHandler.post(winGame);
                                            mHandler.post(clearWord);
                                            myCount = 10;
                                            roundCnt--;
                                            roundStart = false;
                                        }
                                        else if (result.equals("3")) { // 점프
                                            mHandler.post(clearWord);
                                            timer.cancel();
                                            myCount = 10;
                                            timer.start();
                                        }
                                        else if (result.equals("4")) { // 백
                                            mHandler.post(clearWord);
                                            timer.cancel();
                                            myCount = 10;
                                            timer.start();
                                        }
                                        else if (result.equals("5")) { // 게임 종료
                                            roundStart = false;
                                            myCount = 10;
                                            nowRound++;
                                            roundCnt--;
                                            mHandler.post(winGame);
                                        }
                                    }
                                    try {
                                        sleep(100);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                timer.cancel();
                                switch (tmpCnt) {
                                    case 1:
                                        mHandler.post(completeProblem1);
                                        tmpCnt++;
                                        break;
                                    case 2:
                                        mHandler.post(completeProblem2);
                                        tmpCnt++;
                                        break;
                                    case 3:
                                        mHandler.post(completeProblem3);
                                        tmpCnt++;
                                        break;
                                    case 4:
                                        mHandler.post(completeProblem4);
                                        tmpCnt++;
                                        break;
                                    case 5:
                                        mHandler.post(completeProblem5);
                                        tmpCnt++;
                                        break;
                                    case 6:
                                        mHandler.post(completeProblem6);
                                        tmpCnt++;
                                        break;
                                }
                                hintCnt = 1; jumpCnt = 1; backCnt = 1; // 아이템 사용 횟수 초기화
                                try {
                                    sleep(2000); // 조금 대기 후 다음 라운드 시작
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            gameStart = false; // 모든 라운드 종료, 다시 게임 대기 상태
                            tmpCnt = 1; // tmpCnt 초기화
                            mHandler.post(outUnlock); // 나가기 버튼 잠금 해제
                            mHandler.post(gameOver);
                            hintCnt = 1; jumpCnt = 1; backCnt = 1; // 아이템 사용 횟수 초기화
                            try {
                                sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException ioe) {
                Log.e("IOException", "123");
            }
            return null;
        }
    }

    // 핸들러를 사용하지 않고도 일정시간마다 (혹은 후에) 코스를 수행할수 있도록
    // CountDownTimer 클래스가 제공된다.
    // '총시간'  과 '인터벌(간격)' 을 주면 매 간격마다 onTick 메소드를 수행한다.
        public class Timer extends CountDownTimer {
            public Timer (long time, long timeGap) {
                super(time, timeGap);
            }

            @Override
            public void onTick(long millisUntilFinished) {
                if (myCount > 0) {
                    myCount--;
                }
                mHandler.post(countDown);
            }

            @Override
        public void onFinish() {

        }
    }

    public class InOutUpdate extends Thread {
        public boolean isUsed = false;
        public void run() {
            try {
                System.out.println("스레드 가동");
                /* 사람 입장, 퇴장에 대한 통신 */
                byte[] a = new byte[1];
                SocketHandler.bis.read(a, 0, 1);
                String inOut = new String(a); // 입장 or 퇴장
                System.out.println("inOut = " + inOut);
                byte[] b = new byte[1];
                SocketHandler.bis.read(b, 0, 1);
                String inOutNumber = new String(b); // 사람의 fixOrder
                System.out.println("inOutNumber = " + inOutNumber);
                byte[] c = new byte[1];
                SocketHandler.bis.read(c, 0, 1);
                String inRoomCnt = new String(c); // 해당 방의 인원 수
                System.out.println("inRoomCnt = " + inRoomCnt);
                byte[] d = new byte[1];
                SocketHandler.bis.read(d, 0, 1);
                roomLeader = new String(d); // 방장
                System.out.println("roomLeader = " + roomLeader);
                if (inOut.equals("1")) { // 입장
                    switch (inOutNumber) {
                        case "0":
                            mHandler.post(setInUser1);
                            break;
                        case "1":
                            mHandler.post(setInUser2);
                            break;
                        case "2":
                            mHandler.post(setInUser3);
                            break;
                        case "3":
                            mHandler.post(setInUser4);
                            break;
                        default:
                            break;
                    }
                }
                else if (inOut.equals("0")) { // 퇴장
                    switch (inOutNumber) {
                        case "0":
                            mHandler.post(setOutUser1);
                            break;
                        case "1":
                            mHandler.post(setOutUser2);
                            break;
                        case "2":
                            mHandler.post(setOutUser3);
                            break;
                        case "3":
                            mHandler.post(setOutUser4);
                            break;
                        default:
                            break;
                    }
                }
                /* dummy */
                else if (inOut.equals("3")) {
                    gameStart = true;
                }
                manCnt = Integer.parseInt(inRoomCnt);
                isUsed = true;
                System.out.println("manCnt : " + manCnt);
                SocketHandler.bos.flush();
                SocketHandler.bis.reset();
            } catch (IOException e) {

            }
            System.out.println("스레드 종료");
        }
    }

    // 유저1 턴
    private Runnable setTurn1 = new Runnable() {
        @Override
        public void run() {
            turn1.setVisibility(View.VISIBLE);
            turn2.setVisibility(View.INVISIBLE);
            turn3.setVisibility(View.INVISIBLE);
            turn4.setVisibility(View.INVISIBLE);
        }
    };

    // 유저2 턴
    private Runnable setTurn2 = new Runnable() {
        @Override
        public void run() {
            turn1.setVisibility(View.INVISIBLE);
            turn2.setVisibility(View.VISIBLE);
            turn3.setVisibility(View.INVISIBLE);
            turn4.setVisibility(View.INVISIBLE);
        }
    };
    // 유저3 턴
    private Runnable setTurn3 = new Runnable() {
        @Override
        public void run() {
            turn1.setVisibility(View.INVISIBLE);
            turn2.setVisibility(View.INVISIBLE);
            turn3.setVisibility(View.VISIBLE);
            turn4.setVisibility(View.INVISIBLE);
        }
    };
    // 유저4 턴
    private Runnable setTurn4 = new Runnable() {
        @Override
        public void run() {
            turn1.setVisibility(View.INVISIBLE);
            turn2.setVisibility(View.INVISIBLE);
            turn3.setVisibility(View.INVISIBLE);
            turn4.setVisibility(View.VISIBLE);
        }
    };

    // 유저1 In
    private Runnable setInUser1 = new Runnable() {
        @Override
        public void run() {
            user1.setVisibility(View.VISIBLE);
        }
    };

    // 유저2 In
    private Runnable setInUser2 = new Runnable() {
        @Override
        public void run() {
            user2.setVisibility(View.VISIBLE);
        }
    };

    // 유저3 In
    private Runnable setInUser3 = new Runnable() {
        @Override
        public void run() {
            user3.setVisibility(View.VISIBLE);
        }
    };

    // 유저4 In
    private Runnable setInUser4 = new Runnable() {
        @Override
        public void run() {
            user4.setVisibility(View.VISIBLE);
        }
    };

    // 유저1 Out
    private Runnable setOutUser1 = new Runnable() {
        @Override
        public void run() {
            user1.setVisibility(View.INVISIBLE);
        }
    };

    // 유저2 Out
    private Runnable setOutUser2 = new Runnable() {
        @Override
        public void run() {
            user2.setVisibility(View.INVISIBLE);
        }
    };

    // 유저3 Out
    private Runnable setOutUser3 = new Runnable() {
        @Override
        public void run() {
            user3.setVisibility(View.INVISIBLE);
        }
    };

    // 유저4 Out
    private Runnable setOutUser4 = new Runnable() {
        @Override
        public void run() {
            user4.setVisibility(View.INVISIBLE);
        }
    };

    /* 단어 정보 얻기 작업 */
    private Runnable getWord = new Runnable() {
        @Override
        public void run() {
            word = inputWord.getText().toString();
        }
    };

    /* 힌트로 얻은 단어 쓰기 작업 */
    private Runnable setHintWord = new Runnable() {
        @Override
        public void run() {
            mHandler.post(setHintWord2);
        }
    };

    /* 힌트로 얻은 단어 쓰기 작업 */
    private Runnable setHintWord2 = new Runnable() {
        @Override
        public void run() {
            System.out.println("run = " + hintWord);
            inputWord.setText(hintWord);
        }
    };

    /* 칠판에 단어 쓰기 작업 */
    private Runnable setWord = new Runnable() {
        @Override
        public void run() {
            inputWord.setText(word);
        }
    };

    /* EditText Clear 작업 */
    private Runnable clearWord = new Runnable() {
        @Override
        public void run() {
            inputWord.getText().clear();
        }
    };

    /* CountDown UI 변경 작업 */
    private Runnable countDown = new Runnable() {
        @Override
        public void run() {
            mText.setText(" " + myCount);
        }
    };

    /* 나가기 버튼 enable 작업 */
    private Runnable outLock = new Runnable() {
        @Override
        public void run() {
            out.setEnabled(false);
        }
    };

    /* 나가기 버튼 able 작업 */
    private Runnable outUnlock = new Runnable() {
        @Override
        public void run() {
            out.setEnabled(true);
        }
    };

    /* 글자 입력 오류 알림 */
    private Runnable wordErrorToast = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(PlayActivity.this, "3글자를 입력하세요!", Toast.LENGTH_SHORT).show();
        }
    };

    /* 아이템 사용 불가 알림 */
    private Runnable haveNotItemToast = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(PlayActivity.this, clickItem + " 아이템을 사용할 수 없습니다. (사용 가능 횟수 0)", Toast.LENGTH_SHORT).show();
        }
    };

    /* 제시어 설정 */
    private Runnable setProblem = new Runnable() {
        @Override
        public void run() {
            problem1.setText(String.valueOf(mDataset[0]));
            problem2.setText(String.valueOf(mDataset[1]));
            problem3.setText(String.valueOf(mDataset[2]));
            problem4.setText(String.valueOf(mDataset[3]));
            problem5.setText(String.valueOf(mDataset[4]));
            problem6.setText(String.valueOf(mDataset[5]));
        }
    };

    /* 제시어 별 게임 종료 시 */
    private Runnable completeProblem1 = new Runnable() {
        @Override
        public void run() {
            complete1.setVisibility(View.VISIBLE);
        }
    };
    private Runnable completeProblem2 = new Runnable() {
        @Override
        public void run() {
            complete2.setVisibility(View.VISIBLE);
        }
    };
    private Runnable completeProblem3 = new Runnable() {
        @Override
        public void run() {
            complete3.setVisibility(View.VISIBLE);
        }
    };
    private Runnable completeProblem4 = new Runnable() {
        @Override
        public void run() {
            complete4.setVisibility(View.VISIBLE);
        }
    };
    private Runnable completeProblem5 = new Runnable() {
        @Override
        public void run() {
            complete5.setVisibility(View.VISIBLE);
        }
    };
    private Runnable completeProblem6 = new Runnable() {
        @Override
        public void run() {
            complete6.setVisibility(View.VISIBLE);
        }
    };

    /* 맞는 단어를 입력했을 때 */
    private Runnable rightAnswer = new Runnable() {
        @Override
        public void run() {
            right.setVisibility(View.VISIBLE);

            new CountDownTimer(2 * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    right.setVisibility(View.INVISIBLE);
                }
            }.start();
        }
    };

    /* 잘못된 단어를 입력했을 때 */
    private Runnable wrongAnswer = new Runnable() {
        @Override
        public void run() {
            wrong.setVisibility(View.VISIBLE);

            new CountDownTimer(1500, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    wrong.setVisibility(View.INVISIBLE);
                }
            }.start();
        }
    };

    /* 게임에 이겼을 때 */
    private Runnable winGame = new Runnable() {
        @Override
        public void run() {
            win.setVisibility(View.VISIBLE);

            new CountDownTimer(1500, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    win.setVisibility(View.GONE);
                }
            }.start();
        }
    };

    /* 게임에서 졌을 때 */
    private Runnable loseGame = new Runnable() {
        @Override
        public void run() {
            lose.setVisibility(View.VISIBLE);

            new CountDownTimer(1500, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    lose.setVisibility(View.GONE);
                }
            }.start();
        }
    };

    /* 모든 라운드가 종료되었을 때 */
    private Runnable gameOver = new Runnable() {
        @Override
        public void run() {
            gameover.setVisibility(View.VISIBLE);

            new CountDownTimer(1500, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    gameover.setVisibility(View.GONE);
                }
            }.start();
        }
    };

    /* Round 변경 쓰레드 */
    private Runnable nextRound = new Runnable() {
        @Override
        public void run() {
            //rdNum.setText(String.valueOf(Integer.parseInt(String.valueOf(rdNum.getText()))+1));
            rdNum.setText(String.valueOf(nowRound));
        }
    };

    /* 이전 사용 단어 출력 */
    private Runnable setPreWord = new Runnable() {
        @Override
        public void run() {
            preWord.setText("이전 단어 : " + word);
        }
    };

    /* 이전 사용 단어 없을 때 */
    private Runnable nullPreWord = new Runnable() {
        @Override
        public void run() {
            preWord.setText("이전 단어 : ");
        }
    };
}