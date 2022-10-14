/*
* Copyright © 2021 Intel Corporation. All Rights Reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
* 3. The name of the author may not be used to endorse or promote products
*    derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
* EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
* OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
* WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
* OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
* ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import UIKit

class ViewController: UIViewController {
    var p2pclient: P2PClient? = nil
    @IBOutlet weak var renderView: UIView!
    @IBOutlet weak var connectView: UIView!
    @IBOutlet weak var serverIpPortField: UITextField!
    @IBOutlet weak var peerIdField: UITextField!
    @IBOutlet weak var clientIdField: UITextField!
    @IBOutlet weak var exitButton: UIButton!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        self.exitButton.isHidden = true
        self.serverIpPortField.text = UserDefaults.standard.string(forKey: "host")
        self.peerIdField.text = UserDefaults.standard.string(forKey: "peer")
        self.clientIdField.text = UserDefaults.standard.string(forKey: "client")
        
        p2pclient = P2PClient()
    }

    @IBAction func conntetClick(_ sender: Any) {
        
        self.serverIpPortField.resignFirstResponder()
        self.peerIdField.resignFirstResponder()
        self.clientIdField.resignFirstResponder()
        
        if let host = self.serverIpPortField.text, let peer = peerIdField.text, let client = clientIdField.text {
            
            UserDefaults.standard.set(host, forKey: "host")
            UserDefaults.standard.set(peer, forKey: "peer")
            UserDefaults.standard.set(client, forKey: "client")
            
            p2pclient?.setRenderView(self.renderView)
            p2pclient?.connect(host, peerId: peer, clientId: client) { [weak self] success in
                if success {
                    self?.connectView.isHidden = true
                    self?.exitButton.isHidden = false
                }
            }
        }
    }
    
    @IBAction func exitClick(_ sender: Any) {
        self.exitButton.isEnabled = false
        self.p2pclient?.disconnect() { [weak self] success in
            self?.p2pclient?.setRenderView(nil)
            self?.connectView.isHidden = false
            self?.exitButton.isHidden = true
            self?.exitButton.isEnabled = true
        }
    }
    
}

