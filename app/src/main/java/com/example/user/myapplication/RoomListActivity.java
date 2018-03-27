package com.example.user.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RoomListActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private RoomListRecyclerAdapter mAdapter;
    private LinearLayoutManager mLayoutManager;

    private roomListTask client;

    private Handler mHandler;
    private SocketHandler pSocket = null;
    private String roomNum, roomLeader;
    private String ip = "192.168.43.121"; // 고정 IP
    private int port = 9090;
    private boolean roomCreate, roomJoin, roomRefresh;
    private String roomCnt, roomList, ingameList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);
        roomCreate = false;
        roomJoin = false;
        roomRefresh = false;
        mHandler = new Handler();

        ImageView back = (ImageView) findViewById(R.id.back_list);
        back.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v) {
                finish();
            }
        });

        Button button1 = (Button) findViewById(R.id.btn_refresh);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomRefresh = true;
            }
        });

        Button button = (Button) findViewById(R.id.btn_make_room);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MakeRoomDialog dialog = new MakeRoomDialog(RoomListActivity.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialog.show();
            }
        });
    }

    @Override
    protected void onStop() {
        System.out.println("onStop");
        super.onStop();
        client.cancel(true);
    }

    @Override
    protected void onResume() {
        System.out.println("onResume");
        super.onResume();

        client = new roomListTask(ip, port);
        client.execute();

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_add_card);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            /* 액티비티 종료 시 AsyncTask 종료 */
            if (client.getStatus() == AsyncTask.Status.RUNNING) {
                client.cancel(true);
            }
            if (pSocket != null) {
                pSocket.socket.close();
                pSocket.bos.close();
                pSocket.bis.close();
                pSocket.socket = null;
                pSocket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public class roomListTask extends AsyncTask<Void, Void, Void> {
        String dstAddress;
        int dstPort;

        roomListTask(String addr, int port) {
            dstAddress = addr;
            dstPort = port;
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                if (pSocket == null) {
                    pSocket = new SocketHandler(dstAddress, dstPort);
                }
                else {
                    pSocket = (SocketHandler) getApplicationContext();
                }
                /* 소켓 연결 후 방의 리스트를 받아옴 */
                byte[] b1 = new byte[1];
                pSocket.bis.read(b1, 0, 1);
                roomCnt = new String(b1, "UTF-8"); // 방의 수
                byte[] b2 = new byte[Integer.parseInt(roomCnt)];
                pSocket.bis.read(b2, 0, Integer.parseInt(roomCnt));
                roomList = new String(b2, "UTF-8"); // 방 리스트
                byte[] b3 = new byte[Integer.parseInt(roomCnt)];
                pSocket.bis.read(b3, 0, Integer.parseInt(roomCnt));
                ingameList = new String(b3, "UTF-8"); // 방의 참가 인원
                /* 방 리스트 UI 작업 */
                mHandler.post(setRoom);

                /* 방 생성, 방 입장 */
                while (isCancelled() == false) {
                    if (roomCreate) {
                        if (roomNum.length() != 1) { // roomNum : 0~9만 허용
                            roomCreate = false;
                            roomLeader = "";
                            roomNum = "";
                            mHandler.post(roomNumErrorToast); // 방 번호 에러
                            continue;
                        }
                        /* 방 생성에 대한 데이터 송신 */
                        String proto = "2" + roomNum;
                        ByteBuffer buffer = ByteBuffer.allocate(2);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        buffer.put(proto.getBytes("UTF-8"));
                        byte[] result = new byte[2];
                        result = buffer.array();
                        buffer.flip();
                        pSocket.bos.write(result);
                        pSocket.bos.flush();

                        /* 방 생성에 대한 데이터 수신 */
                        byte[] c1 = new byte[1];
                        pSocket.bis.read(c1, 0, 1);
                        String isCreate = new String(c1);
                        System.out.println("lsCreate : " + isCreate);
                        if (isCreate.equals("1")) {
                            roomCreate = false;
                            Intent intent = new Intent(getApplicationContext(), PlayActivity.class);
                            intent.putExtra("num", roomNum);
                            intent.putExtra("leader", roomLeader);
                            getApplicationContext().startActivity(intent);
                        } else {
                            roomCreate = false;
                            roomLeader = "";
                            roomNum = "";
                            mHandler.post(roomNumExistToast); // 방 번호 중복
                            continue;
                        }
                    } else if (roomJoin) {
                        /* 방 입장에 대한 데이터 송신 */
                        String proto = "3" + roomNum; // 방 입장 요청
                        System.out.println(roomNum);
                        ByteBuffer buffer = ByteBuffer.allocate(2);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        buffer.put(proto.getBytes("UTF-8"));
                        byte[] result = new byte[2];
                        result = buffer.array();
                        buffer.flip();
                        pSocket.bos.write(result);
                        pSocket.bos.flush();

                        /* 방 입장에 대한 데이터 수신 */
                        byte[] c1 = new byte[2];
                        pSocket.bis.read(c1, 0, 2);
                        String tmp = new String(c1);
                        String isJoin = tmp.substring(0,1);
                        roomLeader = tmp.substring(1);

                        if (isJoin.equals("1")) {
                            roomJoin = false;
                            Intent intent = new Intent(getApplicationContext(), PlayActivity.class);
                            intent.putExtra("num", roomNum);
                            intent.putExtra("leader", roomLeader);
                            getApplicationContext().startActivity(intent);
                        } else {
                            roomJoin = false;
                            roomLeader = "";
                            roomNum = "";
                            mHandler.post(fullAndNotExistToast); // 정원이 가득찼거나 존재하지 않는 방
                            continue;
                        }
                    } else if (roomRefresh) {
                        roomRefresh = false;
                        /* 방 새로고침에 대한 데이터 송신 */
                        String proto = "8"; // 방 새로고침 요청
                        ByteBuffer buffer = ByteBuffer.allocate(1);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        buffer.put(proto.getBytes("UTF-8"));
                        byte[] result = new byte[1];
                        result = buffer.array();
                        buffer.flip();
                        pSocket.bos.write(result);
                        pSocket.bos.flush();

                        /* 방 새로고침에 대한 데이터 수신 */
                        byte[] c1 = new byte[1];
                        pSocket.bis.read(c1, 0, 1);
                        roomCnt = new String(c1, "UTF-8"); // 방의 수
                        byte[] c2 = new byte[Integer.parseInt(roomCnt)];
                        pSocket.bis.read(c2, 0, Integer.parseInt(roomCnt));
                        roomList = new String(c2, "UTF-8"); // 방 리스트
                        byte[] c3 = new byte[Integer.parseInt(roomCnt)];
                        pSocket.bis.read(c3, 0, Integer.parseInt(roomCnt));
                        ingameList = new String(c3, "UTF-8"); // 방의 참가 인원
                        /* 방 리스트 UI 작업 */
                        mHandler.post(setRoom);
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (UnknownHostException uhe) {
                Log.e("UnknownHostException", "123");
            } catch (ConnectException ce) {
                mHandler.post(notExistServer);
            } catch (IOException ioe) {
                Log.e("IOException", "123");
            } catch (RuntimeException re) {
                mHandler.post(notExistServer);
            }
            return null;
        }
    }

    class MakeRoomDialog extends Dialog {

        private Context mContext;

        private TextView mTextCamera, mTextStore;

        public MakeRoomDialog(Context context) {
            super(context);
            mContext = context;
        }

        public MakeRoomDialog(Context context, int theme) {
            super(context, theme);
            mContext = context;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_make_room);

            Button btn_ok, btn_no;
            btn_ok = (Button) findViewById(R.id.btn_ok);
            btn_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    roomCreate = true;
                    EditText room = (EditText) findViewById(R.id.roomNum);
                    roomNum = room.getText().toString();
                    roomLeader = "0";
                    MakeRoomDialog.this.dismiss();
                }
            });
            btn_no = (Button) findViewById(R.id.btn_no);
            btn_no.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MakeRoomDialog.this.dismiss();
                }
            });
        }
    }


    private void initRecycler() {
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayout.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new RoomListRecyclerAdapter();
        mRecyclerView.setAdapter(mAdapter);
    }

    class RoomListRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private char[] mDataset = roomList.toCharArray();
        private char[] mDataset2 = ingameList.toCharArray();

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder viewHolder = null;
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_card, parent, false);
            viewHolder = new ItemViewHolder(view);
            return viewHolder;
        }


        @Override
        public int getItemCount() {
            return Integer.parseInt(roomCnt);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ItemViewHolder viewHolder = (ItemViewHolder) holder;
            viewHolder.mTextView.setText((String.valueOf(mDataset[position])));
            viewHolder.inGame.setText(("인원 수 : " + String.valueOf(mDataset2[position]) + "/4"));
        }

        public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView mTextView;
            TextView inGame;

            public ItemViewHolder(View itemView) {
                super(itemView);
                mTextView = (TextView) itemView.findViewById(R.id.itemNum);
                inGame = (TextView) itemView.findViewById(R.id.ingame);
                itemView.setOnClickListener(this);
            }

            public void onClick(View view) {
                roomJoin = true;
                roomNum = String.valueOf(mDataset[mRecyclerView.getChildLayoutPosition(view)]);
            }
        }

    }

    /* 방 번호는 0~9까지만 가능하다는 알림 */
    private Runnable roomNumErrorToast = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(RoomListActivity.this, "방 번호의 가능 범위는 0~9 입니다.", Toast.LENGTH_SHORT).show();
        }
    };

    /* 중복되는 방 반호 존재하다는 알림 */
    private Runnable roomNumExistToast = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(RoomListActivity.this, "이미 개설된 방입니다.", Toast.LENGTH_SHORT).show();
        }
    };

    /* 해당 방 입장 불가 알림 */
    private Runnable fullAndNotExistToast = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(RoomListActivity.this, "방에 정원이 가득차있거나 존재하지 않는 방입니다.", Toast.LENGTH_SHORT).show();
        }
    };

    /* 서버 비구동 알림 */
    private Runnable notExistServer = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(RoomListActivity.this, "서버가 구동되지 않았습니다.", Toast.LENGTH_SHORT).show();
        }
    };

    /* 방 목록 UI 작업 */
    private Runnable setRoom = new Runnable() {
        @Override
        public void run() {
            initRecycler();
        }
    };
}