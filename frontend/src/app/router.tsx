import { createBrowserRouter, ScrollRestoration } from 'react-router';
import { Result } from 'antd';
import { RequirePermission } from '@/core/auth/RequirePermission';
import { PERMISSIONS } from '@/core/auth/permissions';
import { taskListScrollKey } from '@/components/project/task-drawer/navigation';
import { SerialNumberPage } from '@/pages/basic-data/serial-number/SerialNumberPage';
import { LeavePage } from '@/pages/leave/LeavePage';
import { DashboardPage } from '@/pages/project/dashboard/DashboardPage';
import { CalendarPage } from '@/pages/project/calendar/CalendarPage';
import { MyTasksPage } from '@/pages/project/my-tasks/MyTasksPage';
import { ProjectDetailPage } from '@/pages/project/project-detail/ProjectDetailPage';
import { ProjectListPage } from '@/pages/project/project-list/ProjectListPage';
import { TimesheetPage } from '@/pages/project/timesheet/TimesheetPage';
import { TaskDetailPage } from '@/pages/project/task-detail/TaskDetailPage';
import { MeetingDetailPage } from '@/pages/project/meeting-detail/MeetingDetailPage';
import { AnnouncementPage } from '@/pages/system/announcement/AnnouncementPage';
import { RolePage } from '@/pages/system/role/RolePage';
import { UserPage } from '@/pages/system/user/UserPage';
import { HomePage } from './HomePage';
import { AppLayout } from './layout/AppLayout';

// 路徑不可用 /api、/auth 開頭，那些會被 vite 轉給 web-server
export const router = createBrowserRouter([
  {
    path: '/',
    element: (
      <>
        <AppLayout />
        <ScrollRestoration getKey={taskListScrollKey} />
      </>
    ),
    children: [
      { index: true, element: <HomePage /> },
      {
        path: 'leave/requests',
        element: (
          <RequirePermission permission={PERMISSIONS.LEAVE_APPLY}>
            <LeavePage key="mine" scope="mine" />
          </RequirePermission>
        ),
      },
      {
        path: 'leave/reviews',
        element: (
          <RequirePermission permission={PERMISSIONS.LEAVE_APPROVE}>
            <LeavePage key="review" scope="review" />
          </RequirePermission>
        ),
      },
      {
        path: 'dashboard',
        element: (
          <RequirePermission permission={PERMISSIONS.PROJECT_READ}>
            <DashboardPage />
          </RequirePermission>
        ),
      },
      {
        path: 'calendar',
        element: (
          <RequirePermission permission={PERMISSIONS.PROJECT_READ}>
            <CalendarPage />
          </RequirePermission>
        ),
      },
      {
        path: 'projects',
        element: (
          <RequirePermission permission={PERMISSIONS.PROJECT_READ}>
            <ProjectListPage />
          </RequirePermission>
        ),
      },
      {
        path: 'projects/:id',
        element: (
          <RequirePermission permission={PERMISSIONS.PROJECT_READ}>
            <ProjectDetailPage />
          </RequirePermission>
        ),
      },
      {
        path: 'projects/:id/meetings/:meetingId',
        element: (
          <RequirePermission permission={PERMISSIONS.PROJECT_READ}>
            <MeetingDetailPage />
          </RequirePermission>
        ),
      },
      {
        path: 'tasks/:taskId',
        element: (
          <RequirePermission permission={PERMISSIONS.PROJECT_READ}>
            <TaskDetailPage />
          </RequirePermission>
        ),
      },
      {
        path: 'my-tasks',
        element: (
          <RequirePermission permission={PERMISSIONS.PROJECT_READ}>
            <MyTasksPage />
          </RequirePermission>
        ),
      },
      {
        path: 'timesheet',
        element: (
          <RequirePermission permission={PERMISSIONS.PROJECT_READ}>
            <TimesheetPage />
          </RequirePermission>
        ),
      },
      {
        path: 'serial-numbers',
        element: (
          <RequirePermission permission={PERMISSIONS.SERIAL_NUMBER_READ}>
            <SerialNumberPage />
          </RequirePermission>
        ),
      },
      {
        path: 'roles',
        element: (
          <RequirePermission permission={PERMISSIONS.ROLE_MANAGE}>
            <RolePage />
          </RequirePermission>
        ),
      },
      {
        path: 'users',
        element: (
          <RequirePermission permission={PERMISSIONS.ROLE_MANAGE}>
            <UserPage />
          </RequirePermission>
        ),
      },
      {
        path: 'announcements',
        element: (
          <RequirePermission permission={PERMISSIONS.NOTIFICATION_SEND}>
            <AnnouncementPage />
          </RequirePermission>
        ),
      },
      { path: '*', element: <Result status="404" title="找不到頁面" /> },
    ],
  },
]);
