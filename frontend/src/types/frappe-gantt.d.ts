// frappe-gantt 沒有附型別，這裡只宣告用到的部分（v1）
declare module 'frappe-gantt' {
  export interface FrappeGanttTask {
    id: string;
    /** 以 innerHTML 顯示在長條上，必須先跳脫 */
    name: string;
    /** YYYY-MM-DD */
    start: string;
    /** YYYY-MM-DD，包含當天 */
    end: string;
    /** 0–100 */
    progress?: number;
    /** 前置任務 id，逗號分隔 */
    dependencies?: string;
    custom_class?: string;
  }

  export type FrappeGanttViewMode = 'Day' | 'Week' | 'Month';

  /** 點長條時的說明框；set_* 都是用 innerHTML 寫入，內容必須先跳脫 */
  export interface FrappeGanttPopupContext {
    task: FrappeGanttTask & { _start: Date; _end: Date };
    set_title: (html: string) => void;
    set_subtitle: (html: string) => void;
    set_details: (html: string) => void;
  }

  export interface FrappeGanttOptions {
    view_mode?: FrappeGanttViewMode;
    language?: string;
    readonly?: boolean;
    readonly_progress?: boolean;
    bar_height?: number;
    padding?: number;
    today_button?: boolean;
    view_mode_select?: boolean;
    scroll_to?: string;
    /** 拖曳任務時是否連帶移動後續任務 */
    move_dependencies?: boolean;
    popup?: ((context: FrappeGanttPopupContext) => void) | false;
    on_click?: (task: FrappeGanttTask) => void;
    on_date_change?: (task: FrappeGanttTask, start: Date, end: Date) => void;
  }

  export default class Gantt {
    constructor(wrapper: HTMLElement, tasks: FrappeGanttTask[], options?: FrappeGanttOptions);
    change_view_mode(mode: FrappeGanttViewMode): void;
  }
}
