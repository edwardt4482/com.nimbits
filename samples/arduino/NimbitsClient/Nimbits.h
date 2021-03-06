/*
 Nimbits Client
 Copyright 2014 nimbits inc.
 http://nimbits.com


 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

#ifndef _Nimbits_h
#define _Nimbits_h

#include <string.h>
#include <stdlib.h>
#include <WString.h>
#include <Ethernet.h>

#include "Arduino.h"



class Nimbits {
	public:
		typedef void (*DataArrivedDelegate)(Nimbits client, String data, float value);
		bool connect(char hostname[], char email[], char apiKey[], char* points[], int port, char clientId[]);
        bool connected();
        void disconnect();
		void monitor();
		void setDataArrivedDelegate(DataArrivedDelegate dataArrivedDelegate);
		void send(String data);

	private:
        String getStringTableItem(int index);
        void sendHandshake(char hostname[], char path[]);
        EthernetClient _client;
        DataArrivedDelegate _dataArrivedDelegate;
        bool readHandshake();
        String readLine();
        char* parseJson(char *jsonString);
};


#endif
