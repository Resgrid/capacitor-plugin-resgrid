import { WebPlugin } from '@capacitor/core';

import type { ResgridPlugin, ResgridPluginStartOptions } from './definitions';

export class ResgridWeb extends WebPlugin implements ResgridPlugin {
  async start(options: ResgridPluginStartOptions): Promise<void> {
    throw new Error('Not implemented on web: ' + options.url);
  }
  async stop(): Promise<void> {
    throw new Error('Not implemented on web: ');
  }
  async showModal(): Promise<void> {
    throw new Error('Not implemented on web: ');
  }
  async checkPermissions(): Promise<PermissionStatus> {
    throw this.unimplemented('Not implemented on web.');
  }

  async requestPermissions(): Promise<PermissionStatus> {
    throw this.unimplemented('Not implemented on web.');
  }
}
