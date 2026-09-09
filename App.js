import React, {useState, useEffect} from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  NativeModules,
  NativeEventEmitter,
  Alert,
  PermissionsAndroid,
  Platform,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

const {BluetoothMonitor} = NativeModules;
const bluetoothEmitter = new NativeEventEmitter(BluetoothMonitor);

function App() {
  const [currentConnection, setCurrentConnection] = useState(null);
  const [savedCarName, setSavedCarName] = useState(null);
  const [connectionLog, setConnectionLog] = useState([]);
  const [backgroundStatus, setBackgroundStatus] = useState('Checking...');

  useEffect(() => {
    let connectListener;
    let disconnectListener;
    let cancelled = false;

    const boot = async () => {
      await requestBluetoothPermission();
      if (cancelled) return;
      await loadSavedData();
      if (cancelled) return;
      await checkCurrentConnection();
      if (cancelled) return;
      BluetoothMonitor.isMonitoringEnabled().then(enabled => {
        setBackgroundStatus(enabled ? 'Active (Foreground Service)' : 'Limited to foreground');
      });
    };

    boot();

    connectListener = bluetoothEmitter.addListener(
      'onBluetoothConnected',
      (event) => {
        setCurrentConnection(event.deviceName);
        if (event.deviceName === savedCarName) {
          addLogEntry('connected', event.deviceName);
        }
      }
    );

    disconnectListener = bluetoothEmitter.addListener(
      'onBluetoothDisconnected',
      (event) => {
        if (event.deviceName === savedCarName) {
          addLogEntry('disconnected', event.deviceName);
        }
        setCurrentConnection(null);
      }
    );

    return () => {
      cancelled = true;
      connectListener && connectListener.remove();
      disconnectListener && disconnectListener.remove();
    };
  }, [savedCarName]);

  const requestBluetoothPermission = async () => {
    if (Platform.OS !== 'android') return true;
    if (Platform.Version < 31) return true;
    try {
      const result = await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
        PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
      ]);
      const connect = result[PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT];
      if (connect !== PermissionsAndroid.RESULTS.GRANTED) {
        Alert.alert(
          'Bluetooth permission',
          'Allow Nearby devices for Car Detector, or it cannot see your car connection.',
        );
        return false;
      }
      return true;
    } catch (e) {
      console.error(e);
      return false;
    }
  };

  const loadSavedData = async () => {
    try {
      const car = await AsyncStorage.getItem('savedCarName');
      const log = await AsyncStorage.getItem('connectionLog');
      if (car) setSavedCarName(car);
      if (log) setConnectionLog(JSON.parse(log));
    } catch (e) {
      console.error('Failed to load saved data:', e);
    }
  };

  const checkCurrentConnection = async () => {
    try {
      const device = await BluetoothMonitor.getCurrentConnection();
      if (device && device.name) {
        setCurrentConnection(device.name);
      } else {
        setCurrentConnection(null);
      }
    } catch (err) {
      console.error(err);
      const msg = (err && (err.message || err.userInfo)) || String(err);
      if (String(msg).includes('PERMISSION') || String(msg).toLowerCase().includes('permission')) {
        Alert.alert(
          'Bluetooth permission',
          'Allow Nearby devices for Car Detector in Android settings.',
        );
      }
      setCurrentConnection(null);
    }
  };

  const markAsCar = async () => {
    if (!currentConnection) {
      Alert.alert('No Connection', 'Connect a Bluetooth device first');
      return;
    }
    
    await AsyncStorage.setItem('savedCarName', currentConnection);
    setSavedCarName(currentConnection);
    addLogEntry('connected', currentConnection);
    
    // Start background monitoring
    BluetoothMonitor.startMonitoring(currentConnection);
  };

  const clearSavedCar = async () => {
    await AsyncStorage.setItem('savedCarName', '');
    setSavedCarName(null);
    BluetoothMonitor.stopMonitoring();
  };

  const addLogEntry = async (event, deviceName) => {
    const entry = {
      id: Date.now(),
      timestamp: new Date().toISOString(),
      event,
      deviceName,
    };
    
    const newLog = [entry, ...connectionLog].slice(0, 100); // Keep last 100 entries
    setConnectionLog(newLog);
    await AsyncStorage.setItem('connectionLog', JSON.stringify(newLog));
  };

  const clearLog = async () => {
    setConnectionLog([]);
    await AsyncStorage.setItem('connectionLog', JSON.stringify([]));
  };

  const formatTimestamp = (isoString) => {
    const date = new Date(isoString);
    return date.toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      second: '2-digit',
      hour12: true,
    });
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <ScrollView contentInsetAdjustmentBehavior="automatic" style={styles.scrollView}>
        <View style={styles.content}>
          <Text style={styles.title}>Car Connection Detector</Text>

          {/* Current Connection */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Current Connection</Text>
            <View style={styles.card}>
              {currentConnection ? (
                <View>
                  <Text style={styles.connectionName}>{currentConnection}</Text>
                  <Text style={styles.connectionType}>Bluetooth Audio</Text>
                  {savedCarName !== currentConnection && (
                    <TouchableOpacity style={styles.button} onPress={markAsCar}>
                      <Text style={styles.buttonText}>Mark as Car</Text>
                    </TouchableOpacity>
                  )}
                  {savedCarName === currentConnection && (
                    <Text style={styles.carBadge}>🚗 This is your car</Text>
                  )}
                </View>
              ) : (
                <Text style={styles.noConnection}>No Bluetooth audio connected</Text>
              )}
              <TouchableOpacity style={[styles.button, {marginTop: 12}]} onPress={checkCurrentConnection}>
                <Text style={styles.buttonText}>Refresh</Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* Saved Car */}
          {savedCarName && (
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>Saved Car Connection</Text>
              <View style={[styles.card, styles.carCard]}>
                <Text style={styles.savedCarName}>🚗 {savedCarName}</Text>
                <TouchableOpacity onPress={clearSavedCar}>
                  <Text style={styles.clearButton}>Clear</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}

          {/* Background Status */}
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Background Monitoring</Text>
            <View style={styles.card}>
              <Text style={styles.backgroundStatus}>
                ● {backgroundStatus}
              </Text>
            </View>
          </View>

          {/* Connection Log */}
          <View style={styles.section}>
            <View style={styles.logHeader}>
              <Text style={styles.sectionTitle}>Connection Log</Text>
              {connectionLog.length > 0 && (
                <TouchableOpacity onPress={clearLog}>
                  <Text style={styles.clearButton}>Clear Log</Text>
                </TouchableOpacity>
              )}
            </View>

            {connectionLog.length === 0 ? (
              <View style={styles.card}>
                <Text style={styles.noConnection}>No events logged yet</Text>
              </View>
            ) : (
              <View>
                {connectionLog.map(entry => (
                  <View key={entry.id} style={styles.logEntry}>
                    <Text style={styles.logEvent}>
                      {entry.event === 'connected' ? '▲' : '▼'} {entry.event === 'connected' ? 'Connected' : 'Disconnected'}
                    </Text>
                    <Text style={styles.logDevice}>{entry.deviceName}</Text>
                    <Text style={styles.logTime}>{formatTimestamp(entry.timestamp)}</Text>
                  </View>
                ))}
              </View>
            )}
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  scrollView: {
    flex: 1,
  },
  content: {
    padding: 16,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
  },
  section: {
    marginBottom: 20,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
    color: '#333',
  },
  card: {
    backgroundColor: 'white',
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  carCard: {
    backgroundColor: '#e3f2fd',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  connectionName: {
    fontSize: 20,
    fontWeight: '600',
    marginBottom: 4,
  },
  connectionType: {
    fontSize: 14,
    color: '#666',
    marginBottom: 12,
  },
  noConnection: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
  },
  button: {
    backgroundColor: '#2196f3',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 8,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  carBadge: {
    fontSize: 16,
    color: '#1976d2',
    marginTop: 8,
    textAlign: 'center',
  },
  savedCarName: {
    fontSize: 16,
    fontWeight: '600',
  },
  clearButton: {
    color: '#f44336',
    fontSize: 14,
    fontWeight: '600',
  },
  backgroundStatus: {
    fontSize: 14,
    color: '#4caf50',
  },
  logHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  logEntry: {
    backgroundColor: 'white',
    borderRadius: 8,
    padding: 12,
    marginBottom: 8,
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
  },
  logEvent: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 4,
  },
  logDevice: {
    fontSize: 14,
    color: '#333',
    marginBottom: 2,
  },
  logTime: {
    fontSize: 12,
    color: '#999',
  },
});

export default App;
