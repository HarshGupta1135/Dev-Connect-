import { useRef, useState } from 'react';
import toast from 'react-hot-toast';
import { errorMessage } from '../api/client';
import { uploadResume } from '../api/endpoints';

const ACCEPT = '.pdf,.doc,.docx';

/** Drag-and-drop resume upload with real progress from the XHR. */
export default function ResumeUpload({ resumeUrl, onUploaded }) {
  const [dragging, setDragging] = useState(false);
  const [progress, setProgress] = useState(null);
  const inputRef = useRef(null);

  const send = async (file) => {
    if (!file) return;

    const name = file.name.toLowerCase();
    if (!['.pdf', '.doc', '.docx'].some((extension) => name.endsWith(extension))) {
      toast.error('Please choose a PDF or Word document.');
      return;
    }

    setProgress(0);
    try {
      const data = await uploadResume(file, setProgress);
      toast.success('Resume uploaded.');
      onUploaded?.(data?.resumeUrl);
    } catch (error) {
      toast.error(errorMessage(error, 'Upload failed.'));
    } finally {
      setProgress(null);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  return (
    <div className="stack" style={{ gap: 10 }}>
      <div
        className="dropzone"
        data-drag={dragging}
        data-busy={progress !== null}
        role="button"
        tabIndex={0}
        onClick={() => inputRef.current?.click()}
        onKeyDown={(event) => {
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            inputRef.current?.click();
          }
        }}
        onDragOver={(event) => {
          event.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(event) => {
          event.preventDefault();
          setDragging(false);
          send(event.dataTransfer.files?.[0]);
        }}
      >
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" style={{ color: 'var(--accent)' }}>
          <path d="M12 16V4M12 4l-4 4M12 4l4 4" />
          <path d="M4 16v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />
        </svg>
        <strong style={{ fontSize: '0.95rem' }}>
          {progress === null ? 'Drop your resume here' : `Uploading… ${progress}%`}
        </strong>
        <span className="tiny faint">PDF or Word, up to a few MB</span>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPT}
          hidden
          onChange={(event) => send(event.target.files?.[0])}
        />
      </div>

      {progress !== null && (
        <div className="progress" role="progressbar" aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100}>
          <span style={{ width: `${progress}%` }} />
        </div>
      )}

      {resumeUrl && progress === null && (
        <div className="panel spread">
          <span className="small">Current resume on file</span>
          <a className="btn btn--soft btn--sm" href={resumeUrl} target="_blank" rel="noreferrer">
            View resume
          </a>
        </div>
      )}
    </div>
  );
}
