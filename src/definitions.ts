export interface ResgridPlugin {
  start(options: ResgridPluginStartOptions): Promise<void>;
  stop(): Promise<void>;
  showModal(): Promise<void>;
  checkPermissions(): Promise<PermissionStatus>;
  requestPermissions(): Promise<PermissionStatus>;
}

export interface ResgridPluginStartOptions {

  token: string;
  
  url: string;
  /**
   * Type of headset to use.
   * 0 = Audio Only, 1 = Video
   * 
   */
  type: number;

  title: string;

  defaultMic: string;

  defaultSpeaker: string;

  apiUrl: string;

  canConnectToVoiceApiToken: string;

  rooms: ResgridPluginRooms[];
}

export interface ResgridPluginRooms {
  name: string;
  id: string;
  token: string;
}