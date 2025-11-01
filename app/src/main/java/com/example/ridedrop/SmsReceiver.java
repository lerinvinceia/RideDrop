package com.example.ridedrop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus.length == 0) return;

            for (Object pdu : pdus) {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);

                String sender = smsMessage.getDisplayOriginatingAddress();
                String messageBody = smsMessage.getMessageBody();

                // You can show Toast to see message
                // Toast.makeText(context, "SMS: " + messageBody, Toast.LENGTH_SHORT).show();

                // Now send this message to activity
                Intent smsIntent = new Intent("otp_received");
                smsIntent.putExtra("message", messageBody);
                context.sendBroadcast(smsIntent);
            }
        }
    }
}
